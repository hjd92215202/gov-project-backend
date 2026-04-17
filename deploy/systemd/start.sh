#!/usr/bin/env bash

set -euo pipefail

APP_HOME="/opt/peoplesAirDefence"
APP_JAR="${APP_HOME}/app.jar"
LOG_DIR="${APP_HOME}/logs"
ETC_DIR="${APP_HOME}/etc"

PROFILE="${SPRING_PROFILES_ACTIVE:-prod}"

if [ ! -f "${APP_JAR}" ]; then
    echo "Error: ${APP_JAR} not found"
    exit 1
fi

mkdir -p "${LOG_DIR}"
chown gov-backend:gov-backend "${LOG_DIR}" 2>/dev/null || true

exec java \
    ${JAVA_OPTS:--Xms2g -Xmx8g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Dfile.encoding=UTF-8} \
    -Dspring.profiles.active="${PROFILE}" \
    -Dspring.config.location=file:${ETC_DIR}/application.yml \
    -Dlogging.file.path="${LOG_DIR}" \
    -jar "${APP_JAR}"
