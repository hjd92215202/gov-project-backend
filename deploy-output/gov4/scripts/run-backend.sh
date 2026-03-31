#!/bin/bash
set -e

if [ ! -f /opt/gov4/backend/app.jar ]; then
  echo "未找到 /opt/gov4/backend/app.jar"
  exit 1
fi

if [ ! -f /opt/gov4/backend/backend.env ]; then
  echo "未找到 /opt/gov4/backend/backend.env"
  exit 1
fi

docker network inspect gov4-net >/dev/null 2>&1 || docker network create gov4-net
docker rm -f gov4-backend >/dev/null 2>&1 || true

docker run -d \
  --name gov4-backend \
  --restart unless-stopped \
  --network gov4-net \
  --env-file /opt/gov4/backend/backend.env \
  -v /opt/gov4/backend/app.jar:/app/app.jar:ro \
  -v /opt/gov4/backend/logs:/app/logs \
  -w /app \
  cr.kylinos.cn/basekylin/java-openjdk-8-aarch64:1.8.0-v10sp1 \
  sh -c 'java ${JAVA_OPTS} -jar /app/app.jar'

echo "后端已启动：gov4-backend"
