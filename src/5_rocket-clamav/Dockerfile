# The pinned tag must be a Debian variant (the Alpine tags are amd64-only, while the Debian
# ones are multi-arch) and must NOT be a `_base` variant: the regular variants ship with virus
# definitions baked in, so that clamd can scan immediately on the first start, even before
# freshclam completes its first update:
FROM clamav/clamav:1.5.2-debian13-slim

# Trust optional custom CA certificates, e.g. of a TLS-intercepting proxy
# (the directory ships empty - see the 'Custom CA Certificates' section of the README).
# freshclam needs these certificates to download virus definitions through such a proxy.
# The `ca-certificates` package is already present in the base image:
COPY ../commons/extra-ca-certificates /usr/local/share/ca-certificates/extra
RUN update-ca-certificates

# Baked-in clamd tuning. The entrypoint of the base image doesn't overwrite this file,
# so it's used by the daemon as is:
COPY ../5_rocket-clamav/clamd.conf /etc/clamav/clamd.conf

# A thin wrapper around the stock entrypoint of the base image (`/init`) that additionally
# installs custom CA certificates mounted at runtime. The wrapper runs as root, while clamd
# itself drops to the unprivileged `clamav` user via the `User` setting in `clamd.conf`.
# The HEALTHCHECK (`clamdcheck.sh`) is inherited from the base image:
COPY ../5_rocket-clamav/rocket-clamav-starter.sh /etc/clamav/rocket-clamav-starter.sh
RUN chmod +x /etc/clamav/rocket-clamav-starter.sh
ENTRYPOINT [ "sh", "-c", "exec /etc/clamav/rocket-clamav-starter.sh" ]
