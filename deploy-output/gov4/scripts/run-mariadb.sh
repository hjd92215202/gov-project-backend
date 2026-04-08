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

MYSQL_ROOT_PASSWORD_VALUE=$(read_env_value "MYSQL_ROOT_PASSWORD" "")

if [ -z "${MYSQL_ROOT_PASSWORD_VALUE}" ] || [ "${MYSQL_ROOT_PASSWORD_VALUE}" = "CHANGE_ME" ]; then
  echo "未配置 MYSQL_ROOT_PASSWORD，请先在 /opt/gov4/backend/backend.env 中填写真实值"
  exit 1
fi

docker network inspect gov4-net >/dev/null 2>&1 || docker network create gov4-net
docker rm -f gov4-mariadb >/dev/null 2>&1 || true

docker run -d \
  --name gov4-mariadb \
  --restart unless-stopped \
  --network gov4-net \
  -e MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD_VALUE}" \
  -v /opt/gov4/mariadb/data:/var/lib/mysql \
  -v /opt/gov4/mariadb/init:/docker-entrypoint-initdb.d \
  cr.kylinos.cn/basekylin/mariadb:10.3.39-ky10sp3_2403 \
  /bin/sh -c '
    set -e

    find_cmd() {
      for cmd in "$@"; do
        if [ -x "${cmd}" ]; then
          printf "%s" "${cmd}"
          return 0
        fi
        if command -v "${cmd}" >/dev/null 2>&1; then
          command -v "${cmd}"
          return 0
        fi
      done
      return 1
    }

    DB_SERVER_CMD=$(find_cmd /usr/libexec/mysqld /usr/sbin/mysqld /usr/libexec/mariadbd /usr/sbin/mariadbd mysqld mariadbd)
    DB_INSTALL_CMD=$(find_cmd mariadb-install-db mysql_install_db)

    if [ -z "${DB_SERVER_CMD}" ] || [ -z "${DB_INSTALL_CMD}" ]; then
      echo "未找到 MariaDB 启动或初始化命令" >&2
      exit 127
    fi

    mkdir -p /var/lib/mysql /run/mariadb /run/mysqld
    chown -R mysql:mysql /var/lib/mysql /run/mariadb /run/mysqld

    if [ ! -d /var/lib/mysql/mysql ]; then
      "${DB_INSTALL_CMD}" --user=mysql --datadir=/var/lib/mysql
    fi

    exec "${DB_SERVER_CMD}" \
      --user=mysql \
      --datadir=/var/lib/mysql \
      --socket=/run/mysqld/mysqld.sock \
      --pid-file=/run/mysqld/mysqld.pid \
      --character-set-server=utf8mb4 \
      --collation-server=utf8mb4_unicode_ci \
      --lower_case_table_names=1
  '

echo "MariaDB 已启动：gov4-mariadb"
