#!/usr/bin/env bash
set -e

: "${PORT:=80}"
: "${APP_PORT:=3001}"

# 1) Nginx 설정 치환
envsubst '$PORT $APP_PORT $FRONTEND_HOST $COOKIE_DOMAIN' \
  < /etc/nginx/conf.d/default.conf.template \
  > /etc/nginx/conf.d/default.conf

# 2) Next 서버 백그라운드 실행 (127.0.0.1 바인딩)
HOSTNAME=127.0.0.1 PORT=$APP_PORT \
  node server.js &

# 3) 준비대기 (최대 20초)
for i in {1..20}; do
  if curl -fs http://127.0.0.1:$APP_PORT >/dev/null; then
    break
  fi
  sleep 1
done

# 4) Nginx 포그라운드
exec nginx -g 'daemon off;'
