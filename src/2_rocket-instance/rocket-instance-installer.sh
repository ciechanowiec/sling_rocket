#!/bin/bash

# Launches a basic Sling App as described at:
# - https://sling.apache.org/documentation/getting-started.html
# - https://github.com/apache/sling-org-apache-sling-starter

SLING_DIR=${SLING_DIR:-/opt/sling}
echo "[INFO] SLING_DIR=$SLING_DIR"
HTTP_PORT="${HTTP_PORT:-8080}"
echo "[INFO] HTTP_PORT=$HTTP_PORT"
mkdir --parents --verbose "$SLING_DIR"
cd "$SLING_DIR" || exec 1

downloadLauncher() {
  latestVersion=$(curl --silent https://repo1.maven.org/maven2/org/apache/sling/org.apache.sling.feature.launcher/maven-metadata.xml | grep '<latest>' | cut -d '>' -f 2 | cut -d '<' -f 1)
  launcherDownloadDir=$(mktemp -d)
  curl --output "$launcherDownloadDir/sling-launcher.zip" "https://repo1.maven.org/maven2/org/apache/sling/org.apache.sling.feature.launcher/$latestVersion/org.apache.sling.feature.launcher-$latestVersion.zip"
  echo "Downloaded Sling Launcher to: $launcherDownloadDir"

  extractionDir=$(mktemp -d)
  yes | unzip "$launcherDownloadDir/sling-launcher.zip" -d "$extractionDir"
  echo "Extracted Sling Launcher to: $extractionDir"

  nestedExtractedDir=$(find "$extractionDir" -mindepth 1 -maxdepth 1 -type d)
  mv --verbose "$nestedExtractedDir"/* "$SLING_DIR/"
}

startSlingInBackground () {
  echo ""
  echo "Sling will be started in the background..."
  ./rocket-instance-starter.sh &
}

setupOakRun () {
echo ""
echo "Resolving Jackrabbit Oak version..."
JACKRABBIT_OAK_VERSION=$(cat JACKRABBIT_OAK_VERSION | xargs)
if [ -z "$JACKRABBIT_OAK_VERSION" ]; then
    echo "Error: Failed to resolve Jackrabbit Oak version"
    exit 1
fi
echo "Resolved Jackrabbit Oak version: $JACKRABBIT_OAK_VERSION"
echo "Downloading Oak Run JAR..."
curl --verbose --remote-name "https://repo1.maven.org/maven2/org/apache/jackrabbit/oak-run/$JACKRABBIT_OAK_VERSION/oak-run-$JACKRABBIT_OAK_VERSION.jar"
cat > "$SLING_DIR/dump-rocket-data.sh" << EOF
#!/bin/bash

echo "[\$(date)] Dumping Sling Rocket data stored at \$SLING_DIR/launcher/repository/segmentstore..."

echo "[\$(date)] Removing old dumps..."
rm -rfv /var/rocket-data-dump/*
mkdir -p /var/rocket-data-dump/backup
mkdir -p /var/rocket-data-dump/export

echo "[\$(date)] Dumping data via backup command..."
java -jar "oak-run-$JACKRABBIT_OAK_VERSION.jar" backup "\$SLING_DIR/launcher/repository/segmentstore" /var/rocket-data-dump/backup
du -sh /var/rocket-data-dump/backup
echo "[\$(date)] Dumping data via export command..."
java -jar "oak-run-$JACKRABBIT_OAK_VERSION.jar" export "\$SLING_DIR/launcher/repository/segmentstore" --blobs true --out /var/rocket-data-dump/export
du -sh /var/rocket-data-dump/export
EOF
echo "Adjusting permissions for the dump script..."
chmod +x "$SLING_DIR/dump-rocket-data.sh"
}

updateActualBundlesStatus () {
  echo ""
  echo "Updating actual bundles status..."
  actualBundlesStatus=$(curl --verbose http://localhost:"$HTTP_PORT"/system/health.json?tags=bundles)
}

waitUntilBundlesStatusMatch () {
  isInitializationFinalized=false
  while [ $isInitializationFinalized = false ]; do
    updateActualBundlesStatus
    date
    echo ""
    echo "Latest logs:"
    tail -n 30 "$SLING_DIR/launcher/logs/error.log"
    echo "Actual bundles status: $actualBundlesStatus"
    echo "Expected bundles status: All [0-9]+ bundles are started"
    if [[ "$actualBundlesStatus" =~ .*All\ [0-9]+\ bundles\ are\ started.* ]]
      then
        isInitializationFinalized=true
        echo "Number of bundles matched"
      else
        echo "Waiting until bundles status match..."
        sleep 10
    fi
  done
}

killSling () {
  echo "Sling process will be terminated..."
  fuser -TERM --namespace tcp --kill "$HTTP_PORT"
  while fuser "$HTTP_PORT"/tcp > /dev/null 2>&1; do
      echo "Latest logs:"
      tail -n 5 "$SLING_DIR/launcher/logs/error.log"
      echo "Waiting for Sling process to be terminated..."
      sleep 5
  done

  echo "Latest logs:"
  tail -n 5 "$SLING_DIR/launcher/logs/error.log"
  echo "Sling process has been terminated"
  sleep 2
}

echo "ROCKET_FEATURE_ARTIFACT_FINAL_NAME=$ROCKET_FEATURE_ARTIFACT_FINAL_NAME"
echo "Setting the static name of the main feature artifact..."
sed -i "s/{{ROCKET_FEATURE_ARTIFACT_FINAL_NAME}}/$ROCKET_FEATURE_ARTIFACT_FINAL_NAME/g" rocket-instance-starter.sh
downloadLauncher
startSlingInBackground # warmup and initialize
waitUntilBundlesStatusMatch
setupOakRun
killSling
