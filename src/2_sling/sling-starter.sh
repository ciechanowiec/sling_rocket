#!/bin/bash

SLING_DIR=${SLING_DIR:-/opt/sling}
echo "[INFO] SLING_DIR=$SLING_DIR"
HTTP_PORT="${HTTP_PORT:-8080}"
echo "[INFO] HTTP_PORT=$HTTP_PORT"
RUN_MODES=${RUN_MODES:-}
echo "[INFO] RUN_MODES=$RUN_MODES"
ENABLE_MULTI_VERSION_SUPPORT=${ENABLE_MULTI_VERSION_SUPPORT:-false}
echo "[INFO] ENABLE_MULTI_VERSION_SUPPORT=$ENABLE_MULTI_VERSION_SUPPORT"
ABS_PATH_TO_ADDITIONAL_FEATURE_ARCHIVE=${ABS_PATH_TO_ADDITIONAL_FEATURE_ARCHIVE:-}
echo "[INFO] ABS_PATH_TO_ADDITIONAL_FEATURE_ARCHIVE=$ABS_PATH_TO_ADDITIONAL_FEATURE_ARCHIVE"

# JAVA_OPTS are set similarly as in https://github.com/apache/sling-org-apache-sling-starter/blob/705420630579652acefe71bb5bdb6229f58ef30a/src/main/container/bin/launch.sh
# Exported JAVA_OPTS will be read by the launcher script generated in https://github.com/apache/sling-org-apache-sling-feature-launcher
if [ ! -z "${JAVA_DEBUG_PORT}" ]; then
    JAVA_DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:${JAVA_DEBUG_PORT}"
fi
# remove add-opens after SLING-10831 is fixed
JAVA_OPTS="--add-opens java.base/java.lang=ALL-UNNAMED ${JAVA_DEBUG_OPTS} ${EXTRA_JAVA_OPTS}"
export JAVA_OPTS
echo "[INFO] JAVA_OPTS=${JAVA_OPTS}"

if [ ! -z "${ABS_PATH_TO_ADDITIONAL_FEATURE_ARCHIVE}" ]; then
    echo "[INFO] Absolute path to the additional feature archive (far) detected. It will be appended to the start command"
    ABS_PATH_TO_ADDITIONAL_FEATURE_ARCHIVE=",$ABS_PATH_TO_ADDITIONAL_FEATURE_ARCHIVE"
fi

cd "$SLING_DIR" || exec 1
# exec is required in order to set the Java process as PID 1 inside the container, since Docker sends
# termination signals only to PID 1, and we need those signals to be handled by the java process:
echo "[INFO] Starting the program..."
exec "$SLING_DIR/bin/launcher" \
     -D sling.run.modes="$RUN_MODES" \
     -D org.osgi.service.http.port="$HTTP_PORT" \
     -D sling.installer.experimental.multiversion="$ENABLE_MULTI_VERSION_SUPPORT" \
     -f "$SLING_DIR/{{ROCKET_FEATURE_ARTIFACT_FINAL_NAME}}$ABS_PATH_TO_ADDITIONAL_FEATURE_ARCHIVE"
