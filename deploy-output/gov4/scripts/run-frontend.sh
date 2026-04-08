#!/bin/bash
set -e

FRONTEND_ENV_FILE="/opt/gov4/frontend/frontend.env"
RUNTIME_DIR="/opt/gov4/frontend/runtime"
RUNTIME_JS_FILE="${RUNTIME_DIR}/env.js"

if [ ! -f /opt/gov4/frontend/dist/index.html ]; then
  echo "未找到 /opt/gov4/frontend/dist/index.html"
  exit 1
fi

if [ ! -f /opt/gov4/frontend/nginx.conf ]; then
  echo "未找到 /opt/gov4/frontend/nginx.conf"
  exit 1
fi

if [ ! -f "${FRONTEND_ENV_FILE}" ]; then
  echo "未找到 ${FRONTEND_ENV_FILE}"
  exit 1
fi

set -a
. "${FRONTEND_ENV_FILE}"
set +a

mkdir -p "${RUNTIME_DIR}"
chmod 755 "${RUNTIME_DIR}"

escape_js_value() {
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g; s/\r//g'
}

{
  printf 'window.__APP_CONFIG__ = {\n'
  first_line=1
  while IFS='=' read -r key value; do
    case "${key}" in
      VITE_*)
        escaped_value=$(escape_js_value "${value}")
        if [ "${first_line}" -eq 0 ]; then
          printf ',\n'
        fi
        printf '  "%s": "%s"' "${key}" "${escaped_value}"
        first_line=0
        ;;
    esac
  done <<EOF
$(env | LC_ALL=C sort)
EOF
  printf '\n};\n'
} > "${RUNTIME_JS_FILE}"

chmod 644 "${RUNTIME_JS_FILE}"

docker network inspect gov4-net >/dev/null 2>&1 || docker network create gov4-net
docker rm -f gov4-frontend >/dev/null 2>&1 || true

docker run -d \
  --name gov4-frontend \
  --restart unless-stopped \
  --network gov4-net \
  -p 81:81 \
  -v /opt/gov4/frontend/dist:/usr/share/nginx/html:ro \
  -v /opt/gov4/frontend/runtime/env.js:/usr/share/nginx/html/env.js:ro \
  -v /opt/gov4/frontend/nginx.conf:/etc/nginx/conf.d/default.conf:ro \
  cr.kylinos.cn/basekylin/nginx:1.21.5-ky10sp3_2403

echo "前端已启动：gov4-frontend（已加载运行时配置，公网仅开放 81 端口）"
