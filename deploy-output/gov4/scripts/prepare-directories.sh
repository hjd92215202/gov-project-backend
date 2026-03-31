#!/bin/bash
set -e

BASE_DIR="/opt/gov4"

mkdir -p "${BASE_DIR}/backend"
mkdir -p "${BASE_DIR}/backend/logs"
mkdir -p "${BASE_DIR}/frontend"
mkdir -p "${BASE_DIR}/frontend/runtime"
mkdir -p "${BASE_DIR}/mariadb/data"
mkdir -p "${BASE_DIR}/mariadb/init"
mkdir -p "${BASE_DIR}/minio/data"
mkdir -p "${BASE_DIR}/scripts"

echo "目录初始化完成：${BASE_DIR}"
