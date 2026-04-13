FROM ubuntu/nginx:1.24-24.04_edge

COPY ../commons/apt-installer.sh .
RUN ./apt-installer.sh
RUN rm apt-installer.sh

# Configuring default ModSecurity
RUN sed -i 's/SecRuleEngine DetectionOnly/SecRuleEngine On/g' /etc/nginx/modsecurity.conf
RUN sed -i 's/^#//' /etc/nginx/modsecurity_includes.conf
RUN echo 'SecRule REQUEST_URI "@beginsWith /login/oauth2/code/google" "id:10001,phase:1,pass,nolog,ctl:ruleRemoveTargetById=930120;ARGS:scope"' >> /etc/nginx/modsecurity_includes.conf
RUN sed -i 's/IncludeOptional/#IncludeOptional/g' /usr/share/modsecurity-crs/owasp-crs.load

# Download and install Nginx Ultimate Bad Bot Blocker files
RUN wget https://raw.githubusercontent.com/mitchellkrogza/nginx-ultimate-bad-bot-blocker/master/install-ngxblocker -O /usr/local/sbin/install-ngxblocker && \
    chmod +x /usr/local/sbin/install-ngxblocker && \
    /usr/local/sbin/install-ngxblocker -x && \
    chmod +x /usr/local/sbin/setup-ngxblocker && \
    chmod +x /usr/local/sbin/update-ngxblocker \
RUN echo '"~*(?:\b)libredtail-http(?:\b)" 3;' >> /etc/nginx/bots.d/blacklist-user-agents.conf

COPY ../3_rocket-nginx/nginx.conf /etc/nginx/nginx.conf
COPY ../3_rocket-nginx/rocket-nginx-starter.sh /etc/nginx/rocket-nginx-starter.sh
RUN chmod +x /etc/nginx/rocket-nginx-starter.sh

COPY ../3_rocket-nginx/error.html /usr/share/nginx/html/error.html

COPY ../3_rocket-nginx/renew-ssl-certs.sh /etc/nginx/renew-ssl-certs.sh
RUN chmod +x /etc/nginx/renew-ssl-certs.sh
RUN echo "0 0 * * * /etc/nginx/renew-ssl-certs.sh >> /var/log/cron.log 2>&1" > /etc/cron.d/dailyjob
RUN echo "0 2 * * * /usr/local/sbin/update-ngxblocker -e botblocker@localhost >> /var/log/cron.log 2>&1" >> /etc/cron.d/dailyjob
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
