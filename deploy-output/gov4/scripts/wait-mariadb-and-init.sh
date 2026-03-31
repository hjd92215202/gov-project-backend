#!/bin/bash
set -e

ENV_FILE="/opt/gov4/backend/backend.env"
SQL_FILE="/opt/gov4/mariadb/init/RBAC.sql"
CONTAINER_NAME="gov4-mariadb"
DB_SOCKET="/run/mysqld/mysqld.sock"
DB_CHARSET_ARGS="--default-character-set=utf8mb4 --protocol=socket --socket=${DB_SOCKET}"

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

MYSQL_DATABASE_NAME=$(read_env_value "GOV_DB_NAME" "gov_db")
MYSQL_USER_NAME=$(read_env_value "GOV_DB_USERNAME" "db_user")
MYSQL_USER_PASSWORD=$(read_env_value "GOV_DB_PASSWORD" "Egov@123")
MYSQL_ROOT_PASSWORD_VALUE=$(read_env_value "MYSQL_ROOT_PASSWORD" "123")

run_client_command() {
  local sql_text="$1"
  docker exec -i -e MYSQL_PWD="${MYSQL_ROOT_PASSWORD_VALUE}" "${CONTAINER_NAME}" /bin/sh -c "
    CLIENT=\$(command -v mariadb || command -v mysql)
    \"\${CLIENT}\" ${DB_CHARSET_ARGS} -uroot -e 'SELECT 1' >/dev/null 2>&1 || unset MYSQL_PWD
    exec \"\${CLIENT}\" ${DB_CHARSET_ARGS} -uroot
  " <<EOF
${sql_text}
EOF
}

wait_for_mariadb() {
  local retries=120
  local i
  for i in $(seq 1 "${retries}"); do
    if docker exec -e MYSQL_PWD="${MYSQL_ROOT_PASSWORD_VALUE}" "${CONTAINER_NAME}" /bin/sh -c "
      CLIENT=\$(command -v mariadb || command -v mysql)
      \"\${CLIENT}\" ${DB_CHARSET_ARGS} -uroot -e 'SELECT 1' >/dev/null 2>&1 || { unset MYSQL_PWD; \"\${CLIENT}\" ${DB_CHARSET_ARGS} -uroot -e 'SELECT 1' >/dev/null 2>&1; }
    "; then
      return 0
    fi
    sleep 2
  done
  return 1
}

schema_initialized() {
  local query
  local result
  query="SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${MYSQL_DATABASE_NAME}' AND table_name='sys_user';"
  result=$(docker exec -e MYSQL_PWD="${MYSQL_ROOT_PASSWORD_VALUE}" "${CONTAINER_NAME}" /bin/sh -c "
    CLIENT=\$(command -v mariadb || command -v mysql)
    RESULT=\$(\"\${CLIENT}\" ${DB_CHARSET_ARGS} -N -B -uroot -e \"${query}\" 2>/dev/null || true)
    if [ -z \"\${RESULT}\" ]; then
      unset MYSQL_PWD
      RESULT=\$(\"\${CLIENT}\" ${DB_CHARSET_ARGS} -N -B -uroot -e \"${query}\")
    fi
    printf '%s' \"\${RESULT}\"
  ")
  [ "${result}" = "1" ]
}

escape_sed_value() {
  printf '%s' "$1" | sed 's/[\/&]/\\&/g'
}

if ! wait_for_mariadb; then
  echo "MariaDB 在预期时间内未就绪" >&2
  exit 1
fi

run_client_command "
ALTER USER 'root'@'localhost' IDENTIFIED BY '${MYSQL_ROOT_PASSWORD_VALUE}';
CREATE DATABASE IF NOT EXISTS \`${MYSQL_DATABASE_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${MYSQL_USER_NAME}'@'%' IDENTIFIED BY '${MYSQL_USER_PASSWORD}';
ALTER USER '${MYSQL_USER_NAME}'@'%' IDENTIFIED BY '${MYSQL_USER_PASSWORD}';
GRANT ALL PRIVILEGES ON \`${MYSQL_DATABASE_NAME}\`.* TO '${MYSQL_USER_NAME}'@'%';
CREATE USER IF NOT EXISTS '${MYSQL_USER_NAME}'@'gov4-backend' IDENTIFIED BY '${MYSQL_USER_PASSWORD}';
ALTER USER '${MYSQL_USER_NAME}'@'gov4-backend' IDENTIFIED BY '${MYSQL_USER_PASSWORD}';
GRANT ALL PRIVILEGES ON \`${MYSQL_DATABASE_NAME}\`.* TO '${MYSQL_USER_NAME}'@'gov4-backend';
CREATE USER IF NOT EXISTS '${MYSQL_USER_NAME}'@'gov4-backend.gov4-net' IDENTIFIED BY '${MYSQL_USER_PASSWORD}';
ALTER USER '${MYSQL_USER_NAME}'@'gov4-backend.gov4-net' IDENTIFIED BY '${MYSQL_USER_PASSWORD}';
GRANT ALL PRIVILEGES ON \`${MYSQL_DATABASE_NAME}\`.* TO '${MYSQL_USER_NAME}'@'gov4-backend.gov4-net';
GRANT SHOW DATABASES, REFERENCES ON *.* TO '${MYSQL_USER_NAME}'@'%';
GRANT SHOW DATABASES, REFERENCES ON *.* TO '${MYSQL_USER_NAME}'@'gov4-backend';
GRANT SHOW DATABASES, REFERENCES ON *.* TO '${MYSQL_USER_NAME}'@'gov4-backend.gov4-net';
FLUSH PRIVILEGES;
"

if schema_initialized; then
  echo "数据库结构已存在，跳过初始 SQL 导入"
  exit 0
fi

if [ ! -f "${SQL_FILE}" ]; then
  echo "未找到初始 SQL 文件：${SQL_FILE}" >&2
  exit 1
fi

DB_NAME_ESCAPED=$(escape_sed_value "${MYSQL_DATABASE_NAME}")
DB_USER_ESCAPED=$(escape_sed_value "${MYSQL_USER_NAME}")

sed \
  -e "s/\<gov_db\>/${DB_NAME_ESCAPED}/g" \
  -e "s/'db_user'@'%'/'${DB_USER_ESCAPED}'@'%'/g" \
  "${SQL_FILE}" | docker exec -i -e MYSQL_PWD="${MYSQL_ROOT_PASSWORD_VALUE}" "${CONTAINER_NAME}" /bin/sh -c "
    CLIENT=\$(command -v mariadb || command -v mysql)
    \"\${CLIENT}\" ${DB_CHARSET_ARGS} -uroot -e 'SELECT 1' >/dev/null 2>&1 || unset MYSQL_PWD
    exec \"\${CLIENT}\" ${DB_CHARSET_ARGS} -uroot
  "

run_client_command "
CREATE USER IF NOT EXISTS '${MYSQL_USER_NAME}'@'gov4-backend' IDENTIFIED BY '${MYSQL_USER_PASSWORD}';
ALTER USER '${MYSQL_USER_NAME}'@'gov4-backend' IDENTIFIED BY '${MYSQL_USER_PASSWORD}';
GRANT ALL PRIVILEGES ON \`${MYSQL_DATABASE_NAME}\`.* TO '${MYSQL_USER_NAME}'@'gov4-backend';
CREATE USER IF NOT EXISTS '${MYSQL_USER_NAME}'@'gov4-backend.gov4-net' IDENTIFIED BY '${MYSQL_USER_PASSWORD}';
ALTER USER '${MYSQL_USER_NAME}'@'gov4-backend.gov4-net' IDENTIFIED BY '${MYSQL_USER_PASSWORD}';
GRANT ALL PRIVILEGES ON \`${MYSQL_DATABASE_NAME}\`.* TO '${MYSQL_USER_NAME}'@'gov4-backend.gov4-net';
FLUSH PRIVILEGES;
"

echo "MariaDB 初始化与授权完成"
