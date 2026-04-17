#!/bin/bash

# 启动脚本 - 支持profile参数
set -e

APP_HOME="/opt/peoplesAirDefence"
APP_JAR="$APP_HOME/app.jar"
LOG_DIR="$APP_HOME/logs"
CONFIG_DIR="$APP_HOME/config"

# 默认profile为prod，可通过环境变量覆盖
PROFILE=${SPRING_PROFILES_ACTIVE:-prod}

# 检查JAR文件
if [ ! -f "$APP_JAR" ]; then
    echo "Error: $APP_JAR not found"
    exit 1
fi

# 创建日志目录
mkdir -p "$LOG_DIR"
chown gov-backend:gov-backend "$LOG_DIR"

# 启动应用
exec java \
    ${JAVA_OPTS:--Xms2g -Xmx8g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Dfile.encoding=UTF-8} \
    -Dspring.profiles.active="$PROFILE" \
    -Dspring.config.location=file:$CONFIG_DIR/application.yml \
    -Dlogging.file.path="$LOG_DIR" \
    -jar "$APP_JAR"
