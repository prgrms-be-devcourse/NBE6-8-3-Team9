FROM node:20-bookworm-slim

RUN apt-get update \
 && apt-get install -y nginx dumb-init gettext-base curl \
 && rm -rf /var/lib/apt/lists/* \
 && rm -f /etc/nginx/sites-enabled/default \
 && rm -f /etc/nginx/conf.d/default.conf \
 && mkdir -p /run/nginx

WORKDIR /app

# Next.js 빌드 산출물 복사 (CI가 미리 생성)
COPY .next/standalone ./
COPY .next ./.next
COPY public ./public

# Nginx 템플릿 & start script
COPY docker/nginx.conf.template /etc/nginx/conf.d/default.conf.template
COPY start.sh /usr/local/bin/start.sh
RUN chmod +x /usr/local/bin/start.sh

ENV NODE_ENV=production \
    APP_PORT=3001

EXPOSE 80
ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD ["/usr/local/bin/start.sh"]
