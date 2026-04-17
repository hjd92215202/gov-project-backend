#!/usr/bin/env bash

set -euo pipefail

ACTION="${1:-}"
RPM_SCRIPTLET_ARG="${2:-1}"

APP_HOME="/opt/peoplesAirDefence"
APP_USER="gov-backend"
APP_GROUP="gov-backend"
ETC_DIR="${APP_HOME}/etc"
DOC_DIR="${APP_HOME}/doc"
EXTRA_FILE_LIST="${DOC_DIR}/extrafilelist.txt"
SERVICE_NAME="gov-backend.service"
SERVICE_ENABLE_LINK="/etc/systemd/system/multi-user.target.wants/${SERVICE_NAME}"

log() {
    echo "[rpm-hooks] $*"
}

ensure_service_user() {
    if id "${APP_USER}" >/dev/null 2>&1; then
        return 0
    fi

    if command -v useradd >/dev/null 2>&1; then
        useradd -r -s /sbin/nologin "${APP_USER}" 2>/dev/null || useradd -r -s /bin/false "${APP_USER}" || true
    fi
}

copy_if_absent() {
    local source_file="$1"
    local target_file="$2"
    local mode="$3"

    if [ -f "${source_file}" ] && [ ! -e "${target_file}" ]; then
        cp "${source_file}" "${target_file}"
        chmod "${mode}" "${target_file}" || true
    fi
}

ensure_permissions() {
    if id "${APP_USER}" >/dev/null 2>&1; then
        chown -R "${APP_USER}:${APP_GROUP}" "${APP_HOME}" 2>/dev/null || true
    fi

    chmod 755 "${APP_HOME}" "${APP_HOME}/bin" "${APP_HOME}/etc" "${APP_HOME}/doc" "${APP_HOME}/data" "${APP_HOME}/logs" 2>/dev/null || true
    chmod 755 "${APP_HOME}/bin/start.sh" "${APP_HOME}/bin/rpm-hooks.sh" 2>/dev/null || true
    chmod 644 "${ETC_DIR}/application.yml" "${ETC_DIR}/application-prod.yml" "${ETC_DIR}/logback-spring.xml" 2>/dev/null || true
    chmod 644 "${ETC_DIR}/application.yml.default" "${ETC_DIR}/application-prod.yml.default" "${ETC_DIR}/logback-spring.xml.default" "${ETC_DIR}/gov-backend.env.example" 2>/dev/null || true
    chmod 600 "${ETC_DIR}/gov-backend.env" 2>/dev/null || true
    chmod 644 "${EXTRA_FILE_LIST}" 2>/dev/null || true
}

refresh_extrafilelist() {
    mkdir -p "${DOC_DIR}"
    touch "${EXTRA_FILE_LIST}"

    local tracked_paths=(
        "/opt/peoplesAirDefence/etc/application.yml"
        "/opt/peoplesAirDefence/etc/application-prod.yml"
        "/opt/peoplesAirDefence/etc/logback-spring.xml"
        "/opt/peoplesAirDefence/etc/gov-backend.env"
        "/etc/systemd/system/multi-user.target.wants/gov-backend.service"
    )

    local path_item
    for path_item in "${tracked_paths[@]}"; do
        if ! grep -Fxq "${path_item}" "${EXTRA_FILE_LIST}" 2>/dev/null; then
            echo "${path_item}" >> "${EXTRA_FILE_LIST}"
        fi
    done
}

do_post() {
    log "running post scriptlet with arg=${RPM_SCRIPTLET_ARG}"

    mkdir -p "${APP_HOME}/bin" "${ETC_DIR}" "${DOC_DIR}" "${APP_HOME}/data" "${APP_HOME}/logs"
    ensure_service_user

    copy_if_absent "${ETC_DIR}/application.yml.default" "${ETC_DIR}/application.yml" 644
    copy_if_absent "${ETC_DIR}/application-prod.yml.default" "${ETC_DIR}/application-prod.yml" 644
    copy_if_absent "${ETC_DIR}/logback-spring.xml.default" "${ETC_DIR}/logback-spring.xml" 644
    copy_if_absent "${ETC_DIR}/gov-backend.env.example" "${ETC_DIR}/gov-backend.env" 600

    refresh_extrafilelist
    ensure_permissions

    systemctl daemon-reload >/dev/null 2>&1 || true
    systemctl enable "${SERVICE_NAME}" >/dev/null 2>&1 || true

    log "service enabled on boot; service is not auto-started by RPM install"
}

do_preun() {
    log "running preun scriptlet with arg=${RPM_SCRIPTLET_ARG}"

    # For rpm scriptlets: 0=erase, 1=upgrade
    if [ "${RPM_SCRIPTLET_ARG}" = "0" ]; then
        systemctl stop "${SERVICE_NAME}" >/dev/null 2>&1 || true
        systemctl disable "${SERVICE_NAME}" >/dev/null 2>&1 || true
    fi
}

do_postun() {
    log "running postun scriptlet with arg=${RPM_SCRIPTLET_ARG}"

    if [ "${RPM_SCRIPTLET_ARG}" = "0" ]; then
        rm -f "${SERVICE_ENABLE_LINK}" >/dev/null 2>&1 || true
    fi
    systemctl daemon-reload >/dev/null 2>&1 || true
}

case "${ACTION}" in
    post)
        do_post
        ;;
    preun)
        do_preun
        ;;
    postun)
        do_postun
        ;;
    *)
        echo "Usage: $0 <post|preun|postun> [rpm_scriptlet_arg]"
        exit 1
        ;;
esac
