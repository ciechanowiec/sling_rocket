#!/bin/bash

DOMAIN=${DOMAIN:-example.com}
EMAIL=${EMAIL:-name@example.com}
IS_STAGING_ENV=${IS_STAGING_ENV:-false}
echo ""
echo "### STARTED ON $(date)"
echo "### DOMAIN: $DOMAIN"
echo "### EMAIL: $EMAIL"
echo "### IS_STAGING_ENV: $IS_STAGING_ENV"

echo "### This domain will be set in Nginx : '$DOMAIN'"
sed -i "s/DOMAIN_PLACEHOLDER/$DOMAIN/g" /etc/nginx/nginx.conf

echo "### Starting Nginx in the background to initialize it..."
nginx &

echo "### Waiting for Nginx to start..."
while ! pgrep -x "nginx" > /dev/null; do
  sleep 1
done

if [ ! -e "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" ]; then
  echo "### Obtaining Let's Encrypt certificate for '$DOMAIN'..."
  CERTBOT_COMMAND="certbot certonly --webroot --webroot-path=/usr/share/nginx/html \
  --email \"$EMAIL\" --agree-tos --no-eff-email -d \"$DOMAIN\""
  if [ "$IS_STAGING_ENV" = "true" ]; then
    echo "### Staging environment detected, using test certificate..."
    CERTBOT_COMMAND="$CERTBOT_COMMAND --test-cert"
  fi
  eval "$CERTBOT_COMMAND"
else
  echo "### Renewing Let's Encrypt certificate for '$DOMAIN'..."
  certbot renew --non-interactive
  echo "### Copying certificates to default locations ..."
  cp -v "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" /etc/ssl/certs/ssl-cert-rocketsite.pem
  cp -v "/etc/letsencrypt/live/$DOMAIN/privkey.pem" /etc/ssl/private/ssl-cert-rocketsite.key
fi

echo "### Copying certificates to default locations ..."
cp -v "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" /etc/ssl/certs/ssl-cert-rocketsite.pem
cp -v "/etc/letsencrypt/live/$DOMAIN/privkey.pem" /etc/ssl/private/ssl-cert-rocketsite.key

echo "### Stopping background Nginx..."
nginx -s quit

echo "### Starting Nginx in the foreground..."
exec nginx -g 'daemon off;'
