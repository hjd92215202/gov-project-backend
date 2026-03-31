#!/bin/bash
set -e

DIR="$(cd "$(dirname "$0")" && pwd)"

sh "${DIR}/prepare-directories.sh"
sh "${DIR}/run-mariadb.sh"
sh "${DIR}/wait-mariadb-and-init.sh"
sh "${DIR}/run-minio.sh"
sh "${DIR}/run-backend.sh"
sh "${DIR}/run-frontend.sh"

echo "全部容器启动完成"
