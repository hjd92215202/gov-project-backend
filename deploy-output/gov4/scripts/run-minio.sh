#!/bin/bash
set -e

ENV_FILE="/opt/gov4/backend/backend.env"

read_env_value() {
  local key="$1"
  local default_value="$2"
  if [ ! -f "${ENV_FILE}" ]; then
    printf '%s' "${default_value}"
    return
  fi
  local matched_line
  matched_line=$(grep -E "^${key}=" "${ENV_FILE}" | tail -n 1 || true)
  if [ -z "${matched_line}" ]; then
    printf '%s' "${default_value}"
    return
  fi
  printf '%s' "${matched_line#*=}" | tr -d '\r'
}

MINIO_ROOT_USER=$(read_env_value "GOV_MINIO_ACCESS_KEY" "")
MINIO_ROOT_PASSWORD=$(read_env_value "GOV_MINIO_SECRET_KEY" "")

if [ -z "${MINIO_ROOT_USER}" ] || [ "${MINIO_ROOT_USER}" = "CHANGE_ME" ]; then
  echo "未配置 GOV_MINIO_ACCESS_KEY，请先在 /opt/gov4/backend/backend.env 中填写真实值"
  exit 1
fi

if [ -z "${MINIO_ROOT_PASSWORD}" ] || [ "${MINIO_ROOT_PASSWORD}" = "CHANGE_ME" ]; then
  echo "未配置 GOV_MINIO_SECRET_KEY，请先在 /opt/gov4/backend/backend.env 中填写真实值"
  exit 1
fi

docker network inspect gov4-net >/dev/null 2>&1 || docker network create gov4-net
docker rm -f gov4-minio >/dev/null 2>&1 || true

docker run -d \
  --name gov4-minio \
  --restart unless-stopped \
  --network gov4-net \
  -p 127.0.0.1:9001:9001 \
  -e MINIO_ROOT_USER="${MINIO_ROOT_USER}" \
  -e MINIO_ROOT_PASSWORD="${MINIO_ROOT_PASSWORD}" \
  -v /opt/gov4/minio/data:/data \
  minio/minio:RELEASE.2023-08-09T23-30-22Z \
  server /data --console-address ":9001"

echo "MinIO 已启动：gov4-minio（控制台仅监听 127.0.0.1:9001）"
