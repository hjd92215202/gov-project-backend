#!/bin/bash
set -u

BASE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND_ENV_FILE="${BASE_DIR}/backend/backend.env"
REPORT_DIR="${BASE_DIR}/reports"
REPORT_FILE="${REPORT_DIR}/runtime-inspect-$(date +%Y%m%d-%H%M%S).log"

mkdir -p "${REPORT_DIR}"

read_env_value() {
  local key="$1"
  local file="$2"
  if [ ! -f "${file}" ]; then
    return 0
  fi
  grep -E "^${key}=" "${file}" | head -n 1 | sed "s/^${key}=//"
}

MYSQL_ROOT_PASSWORD_VALUE="$(read_env_value "MYSQL_ROOT_PASSWORD" "${BACKEND_ENV_FILE}")"
HTTP_PORT_VALUE="$(read_env_value "GOV_HTTP_PORT" "${BACKEND_ENV_FILE}")"

MYSQL_ROOT_PASSWORD_VALUE="${MYSQL_ROOT_PASSWORD_VALUE:-123}"
HTTP_PORT_VALUE="${HTTP_PORT_VALUE:-81}"

print_section() {
  echo
  echo "============================================================"
  echo "$1"
  echo "============================================================"
}

run_cmd() {
  local title="$1"
  shift
  echo
  echo ">>> ${title}"
  echo "+ $*"
  "$@" 2>&1 || echo "[WARN] command failed: $*"
}

run_shell() {
  local title="$1"
  local command="$2"
  echo
  echo ">>> ${title}"
  echo "+ ${command}"
  /bin/sh -c "${command}" 2>&1 || echo "[WARN] command failed: ${command}"
}

{
  echo "Gov4 Runtime Inspection Report"
  echo "Generated At: $(date '+%Y-%m-%d %H:%M:%S %Z')"
  echo "Hostname: $(hostname 2>/dev/null || echo unknown)"
  echo "Base Dir: ${BASE_DIR}"
  echo "HTTP Port: ${HTTP_PORT_VALUE}"

  print_section "Host Basic"
  run_cmd "uname -a" uname -a
  run_cmd "uptime" uptime
  run_cmd "free -h" free -h
  run_cmd "df -h" df -h
  run_cmd "ip addr" ip addr
  run_cmd "ip route" ip route
  run_cmd "ss -lntp" ss -lntp

  print_section "Host Runtime"
  if command -v vmstat >/dev/null 2>&1; then
    run_cmd "vmstat 1 5" vmstat 1 5
  else
    echo "[INFO] vmstat not found"
  fi
  if command -v iostat >/dev/null 2>&1; then
    run_cmd "iostat -x 1 3" iostat -x 1 3
  else
    echo "[INFO] iostat not found"
  fi
  if command -v top >/dev/null 2>&1; then
    run_shell "top -b -n 1 | head -n 30" "top -b -n 1 | head -n 30"
  else
    echo "[INFO] top not found"
  fi

  print_section "Docker Overview"
  run_cmd "docker ps -a" docker ps -a
  run_cmd "docker stats --no-stream" docker stats --no-stream
  run_cmd "docker network ls" docker network ls

  print_section "Container Inspect"
  for container_name in gov4-frontend gov4-backend gov4-minio gov4-mariadb; do
    echo
    echo "--- ${container_name} ---"
    if docker ps -a --format '{{.Names}}' | grep -qx "${container_name}"; then
      run_shell "docker inspect ${container_name}" "docker inspect ${container_name} --format '{{json .State}} {{json .HostConfig.RestartPolicy}} {{json .NetworkSettings.Networks}}'"
      run_cmd "docker logs --tail 120 ${container_name}" docker logs --tail 120 "${container_name}"
    else
      echo "[WARN] container not found: ${container_name}"
    fi
  done

  print_section "HTTP Checks"
  run_shell "curl home" "curl -I -sS --connect-timeout 5 http://127.0.0.1:${HTTP_PORT_VALUE}/"
  run_shell "curl api doc" "curl -I -sS --connect-timeout 5 http://127.0.0.1:${HTTP_PORT_VALUE}/api/doc.html"
  run_shell "curl home timing" "curl -o /dev/null -sS -w 'home total=%{time_total}s code=%{http_code}\n' --connect-timeout 5 http://127.0.0.1:${HTTP_PORT_VALUE}/"
  run_shell "curl api doc timing" "curl -o /dev/null -sS -w 'doc total=%{time_total}s code=%{http_code}\n' --connect-timeout 5 http://127.0.0.1:${HTTP_PORT_VALUE}/api/doc.html"

  print_section "Backend JVM"
  if docker ps -a --format '{{.Names}}' | grep -qx "gov4-backend"; then
    run_shell "backend java process" "docker exec gov4-backend /bin/sh -c 'ps -ef | grep java | grep -v grep'"
    run_shell "backend java flags" "docker exec gov4-backend /bin/sh -c 'tr \"\\0\" \" \" < /proc/1/cmdline'"
    run_shell "backend memory" "docker exec gov4-backend /bin/sh -c 'cat /proc/1/status | grep -E \"VmRSS|VmSize|Threads\"'"
    run_shell "backend jstat" "docker exec gov4-backend /bin/sh -c 'if command -v jstat >/dev/null 2>&1; then JAVA_PID=\$(ps -ef | awk \"/java/ && !/awk/ {print \\\$2; exit}\"); [ -n \"\$JAVA_PID\" ] && jstat -gcutil \"\$JAVA_PID\" 1000 5; else echo jstat_not_found; fi'"
  else
    echo "[WARN] gov4-backend container not found"
  fi

  print_section "MariaDB Checks"
  if docker ps -a --format '{{.Names}}' | grep -qx "gov4-mariadb"; then
    run_shell "mariadb process" "docker exec gov4-mariadb /bin/sh -c 'ps -ef | grep -E \"mysqld|mariadbd\" | grep -v grep'"
    run_shell "mariadb ping" "docker exec gov4-mariadb /bin/sh -c 'CLIENT=\$(command -v mariadb || command -v mysql); MYSQL_PWD=\"${MYSQL_ROOT_PASSWORD_VALUE}\" \$CLIENT --protocol=socket --socket=/run/mysqld/mysqld.sock -uroot -e \"SELECT VERSION() AS version, NOW() AS now_time;\"'"
    run_shell "mariadb status" "docker exec gov4-mariadb /bin/sh -c 'CLIENT=\$(command -v mariadb || command -v mysql); MYSQL_PWD=\"${MYSQL_ROOT_PASSWORD_VALUE}\" \$CLIENT --protocol=socket --socket=/run/mysqld/mysqld.sock -uroot -e \"SHOW GLOBAL STATUS LIKE \\\"Threads_connected\\\"; SHOW GLOBAL STATUS LIKE \\\"Threads_running\\\"; SHOW GLOBAL STATUS LIKE \\\"Questions\\\"; SHOW GLOBAL STATUS LIKE \\\"Slow_queries\\\"; SHOW GLOBAL STATUS LIKE \\\"Uptime\\\";\"'"
    run_shell "mariadb processlist" "docker exec gov4-mariadb /bin/sh -c 'CLIENT=\$(command -v mariadb || command -v mysql); MYSQL_PWD=\"${MYSQL_ROOT_PASSWORD_VALUE}\" \$CLIENT --protocol=socket --socket=/run/mysqld/mysqld.sock -uroot -e \"SHOW FULL PROCESSLIST;\"'"
    run_shell "mariadb table count" "docker exec gov4-mariadb /bin/sh -c 'CLIENT=\$(command -v mariadb || command -v mysql); MYSQL_PWD=\"${MYSQL_ROOT_PASSWORD_VALUE}\" \$CLIENT --protocol=socket --socket=/run/mysqld/mysqld.sock -uroot -e \"SELECT table_schema, COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema IN (\\\"gov_db\\\") GROUP BY table_schema;\"'"
  else
    echo "[WARN] gov4-mariadb container not found"
  fi

  print_section "MinIO Checks"
  if docker ps -a --format '{{.Names}}' | grep -qx "gov4-minio"; then
    run_shell "minio process" "docker exec gov4-minio /bin/sh -c 'ps -ef | grep minio | grep -v grep'"
    run_shell "minio data dir" "docker exec gov4-minio /bin/sh -c 'du -sh /data 2>/dev/null || ls -lah /data'"
  else
    echo "[WARN] gov4-minio container not found"
  fi
} | tee "${REPORT_FILE}"

echo
echo "Report saved to: ${REPORT_FILE}"
