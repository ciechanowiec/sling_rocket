#!/bin/bash

DOMAIN=${DOMAIN:-example.com}
echo "### DOMAIN: $DOMAIN"

echo "### Renewing Let's Encrypt certificate for '$DOMAIN'..."
certbot renew --non-interactive

echo "### Copying certificates to default locations ..."
cp -v "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" /etc/ssl/certs/ssl-cert-rocketsite.pem
cp -v "/etc/letsencrypt/live/$DOMAIN/privkey.pem" /etc/ssl/private/ssl-cert-rocketsite.key

echo "### Reloading Nginx..."
nginx -s reload
