FROM ubuntu/nginx:1.18-22.04_edge

COPY ../commons/apt-installer.sh .
RUN ./apt-installer.sh
RUN rm apt-installer.sh

COPY ../3_rocket-nginx/nginx.conf /etc/nginx/nginx.conf
COPY ../3_rocket-nginx/rocket-nginx-starter.sh /etc/nginx/rocket-nginx-starter.sh
RUN chmod +x /etc/nginx/rocket-nginx-starter.sh

COPY ../3_rocket-nginx/error.html /usr/share/nginx/html/error.html

COPY ../3_rocket-nginx/renew-ssl-certs.sh /etc/nginx/renew-ssl-certs.sh
RUN chmod +x /etc/nginx/renew-ssl-certs.sh
RUN echo "0 0 * * * /etc/nginx/renew-ssl-certs.sh >> /var/log/cron.log 2>&1" > /etc/cron.d/dailyjob
RUN chmod 0644 /etc/cron.d/dailyjob

# Generate self-signed certificates (required for initial nginx start)
RUN mkdir -p /etc/ssl/private /etc/ssl/certs && \
    openssl req -x509 -nodes -newkey rsa:4096 \
    -keyout /etc/ssl/private/ssl-cert-rocketsite.key \
    -out /etc/ssl/certs/ssl-cert-rocketsite.pem \
    -days 1 \
    -subj "/CN=localhost"

WORKDIR /etc/nginx

ENTRYPOINT [ "sh", "-c", "exec /etc/nginx/rocket-nginx-starter.sh" ]
