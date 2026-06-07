#!/bin/sh

# Install optional custom CA certificates mounted at runtime, e.g. of a TLS-intercepting proxy
# (see the 'Custom CA Certificates' section of the README). freshclam needs these certificates
# to download virus definitions through such a proxy:
update-ca-certificates

# Hand off to the stock entrypoint of the base image as PID 1:
exec /init "$@"
