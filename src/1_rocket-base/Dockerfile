FROM eclipse-temurin:25-jdk

COPY ../commons/apt-installer.sh .
RUN ./apt-installer.sh
RUN rm apt-installer.sh

# Trust optional custom CA certificates, e.g. of a TLS-intercepting proxy
# (the directory ships empty - see the 'Custom CA Certificates' section of the README). Must run after the apt
# setup above, which ensures that the `ca-certificates` package is installed:
COPY ../commons/extra-ca-certificates /usr/local/share/ca-certificates/extra
RUN update-ca-certificates
# The JVM uses its own trust store, not the system one, so the custom CA certificates
# must be additionally imported into the Java keystore:
RUN for CERT in /usr/local/share/ca-certificates/extra/*.crt; do \
      if [ -f "$CERT" ]; then \
        keytool -importcert -noprompt -cacerts -storepass changeit \
                -alias "extra-$(basename "$CERT" .crt)" -file "$CERT"; \
      fi; \
    done
