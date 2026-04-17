#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
SPEC_FILE="${REPO_ROOT}/deploy/systemd/gov-backend.spec"
RPM_TOPDIR="${REPO_ROOT}/target/rpmbuild"
DEFAULT_OUTPUT_DIR="${REPO_ROOT}/target/rpm-artifacts"

VERSION=""
RELEASE=""
SIGN="false"
GPG_KEY_NAME=""
OUTPUT_DIR="${DEFAULT_OUTPUT_DIR}"

usage() {
    cat <<'EOF'
Usage: build_arm_rpm.sh --version <version> --release <release> [--sign true|false] [--gpg-key-name <name>] [--output-dir <dir>]

Required:
  --version         RPM version (e.g. 1.0.0)
  --release         RPM release (e.g. 1)

Optional:
  --sign            true|false, default false
  --gpg-key-name    GPG key name for rpmsign
  --output-dir      output directory for rpm, sha256 and manifest
EOF
}

fail() {
    echo "[build_arm_rpm] ERROR: $*" >&2
    exit 1
}

log() {
    echo "[build_arm_rpm] $*"
}

parse_args() {
    while [ $# -gt 0 ]; do
        case "$1" in
            --version)
                VERSION="${2:-}"
                shift 2
                ;;
            --release)
                RELEASE="${2:-}"
                shift 2
                ;;
            --sign)
                SIGN="${2:-false}"
                shift 2
                ;;
            --gpg-key-name)
                GPG_KEY_NAME="${2:-}"
                shift 2
                ;;
            --output-dir)
                OUTPUT_DIR="${2:-}"
                shift 2
                ;;
            -h|--help)
                usage
                exit 0
                ;;
            *)
                fail "Unknown argument: $1"
                ;;
        esac
    done
}

require_cmd() {
    local cmd="$1"
    command -v "${cmd}" >/dev/null 2>&1 || fail "Command not found: ${cmd}"
}

check_environment() {
    local arch
    arch="$(uname -m)"
    [ "${arch}" = "aarch64" ] || fail "ARM build only. Current arch is ${arch}, expected aarch64."

    require_cmd mvn
    require_cmd rpmbuild
    require_cmd rpm
    require_cmd tar
    require_cmd sha256sum
    if [ "${SIGN}" = "true" ]; then
        require_cmd gpg
    fi
}

build_jar() {
    log "Building precompiled jar with Maven"
    (cd "${REPO_ROOT}" && mvn -DskipTests clean package)
}

prepare_rpmbuild_tree() {
    log "Preparing rpmbuild tree: ${RPM_TOPDIR}"
    rm -rf "${RPM_TOPDIR}"
    mkdir -p "${RPM_TOPDIR}/BUILD" "${RPM_TOPDIR}/RPMS" "${RPM_TOPDIR}/SOURCES" "${RPM_TOPDIR}/SPECS" "${RPM_TOPDIR}/SRPMS"
}

prepare_source_tarball() {
    local src_dir tar_name jar_source
    src_dir="${REPO_ROOT}/target/rpm-src/${VERSION}/gov-project-backend-${VERSION}"
    tar_name="gov-project-backend-${VERSION}.tar.gz"
    jar_source="$(find "${REPO_ROOT}/target" -maxdepth 1 -type f -name "gov-project-backend-*.jar" ! -name "*.original" | head -n 1)"

    [ -n "${jar_source}" ] || fail "Built jar not found in ${REPO_ROOT}/target"

    rm -rf "${REPO_ROOT}/target/rpm-src/${VERSION}"
    mkdir -p "${src_dir}/deploy/systemd" "${src_dir}/src/main/resources" "${src_dir}/target"

    cp "${jar_source}" "${src_dir}/target/gov-project-backend-${VERSION}.jar"
    cp "${REPO_ROOT}/deploy/systemd/gov-backend.service" "${src_dir}/deploy/systemd/"
    cp "${REPO_ROOT}/deploy/systemd/gov-backend.env.example" "${src_dir}/deploy/systemd/"
    cp "${REPO_ROOT}/deploy/systemd/start.sh" "${src_dir}/deploy/systemd/"
    cp "${REPO_ROOT}/deploy/systemd/rpm-hooks.sh" "${src_dir}/deploy/systemd/"
    cp "${REPO_ROOT}/deploy/systemd/extrafilelist.txt" "${src_dir}/deploy/systemd/"
    cp "${REPO_ROOT}/src/main/resources/application.yml" "${src_dir}/src/main/resources/"
    cp "${REPO_ROOT}/src/main/resources/application-prod.yml" "${src_dir}/src/main/resources/"
    cp "${REPO_ROOT}/src/main/resources/logback-spring.xml" "${src_dir}/src/main/resources/"

    (cd "${REPO_ROOT}/target/rpm-src/${VERSION}" && tar -czf "${RPM_TOPDIR}/SOURCES/${tar_name}" "gov-project-backend-${VERSION}")
    cp "${SPEC_FILE}" "${RPM_TOPDIR}/SPECS/gov-backend.spec"
}

build_rpm() {
    log "Running rpmbuild"
    rpmbuild -bb "${RPM_TOPDIR}/SPECS/gov-backend.spec" \
        --define "_topdir ${RPM_TOPDIR}" \
        --define "pkg_version ${VERSION}" \
        --define "pkg_release ${RELEASE}"
}

resolve_rpm_path() {
    find "${RPM_TOPDIR}/RPMS" -type f -name "gov-project-backend-${VERSION}-${RELEASE}*.aarch64.rpm" | head -n 1
}

sign_rpm_if_requested() {
    local rpm_path="$1"
    local signed="unsigned"

    if [ "${SIGN}" != "true" ]; then
        echo "${signed}"
        return 0
    fi

    if ! command -v rpmsign >/dev/null 2>&1; then
        log "rpmsign not found; continue without signing"
        echo "${signed}"
        return 0
    fi

    if [ -z "${GPG_KEY_NAME}" ]; then
        log "GPG key name not provided; continue without signing"
        echo "${signed}"
        return 0
    fi

    if ! gpg --list-secret-keys "${GPG_KEY_NAME}" >/dev/null 2>&1; then
        log "GPG secret key '${GPG_KEY_NAME}' not found; continue without signing"
        echo "${signed}"
        return 0
    fi

    log "Signing rpm with key '${GPG_KEY_NAME}'"
    rpmsign --define "_gpg_name ${GPG_KEY_NAME}" --addsign "${rpm_path}"
    signed="signed"
    echo "${signed}"
}

verify_and_export() {
    local rpm_path="$1"
    local sign_state="$2"
    local rpm_file rpm_kv_file sha_file manifest_file

    mkdir -p "${OUTPUT_DIR}"
    rpm_file="${OUTPUT_DIR}/$(basename "${rpm_path}")"
    cp "${rpm_path}" "${rpm_file}"

    rpm_kv_file="${OUTPUT_DIR}/$(basename "${rpm_path}").kv.txt"
    rpm -Kv "${rpm_file}" | tee "${rpm_kv_file}" >/dev/null

    sha_file="${OUTPUT_DIR}/$(basename "${rpm_path}").sha256"
    sha256sum "${rpm_file}" | tee "${sha_file}" >/dev/null

    manifest_file="${OUTPUT_DIR}/manifest.txt"
    cat > "${manifest_file}" <<EOF
name=gov-project-backend
version=${VERSION}
release=${RELEASE}
arch=aarch64
rpm_file=$(basename "${rpm_file}")
signature=${sign_state}
kv_report=$(basename "${rpm_kv_file}")
sha256_file=$(basename "${sha_file}")
built_at=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
EOF

    log "Artifacts exported to ${OUTPUT_DIR}"
}

main() {
    parse_args "$@"

    [ -n "${VERSION}" ] || fail "--version is required"
    [ -n "${RELEASE}" ] || fail "--release is required"
    [ "${SIGN}" = "true" ] || [ "${SIGN}" = "false" ] || fail "--sign must be true or false"
    [ -f "${SPEC_FILE}" ] || fail "Spec file not found: ${SPEC_FILE}"

    check_environment
    build_jar
    prepare_rpmbuild_tree
    prepare_source_tarball
    build_rpm

    local rpm_path
    rpm_path="$(resolve_rpm_path)"
    [ -n "${rpm_path}" ] || fail "RPM package not found under ${RPM_TOPDIR}/RPMS"

    local sign_state
    sign_state="$(sign_rpm_if_requested "${rpm_path}")"

    verify_and_export "${rpm_path}" "${sign_state}"
    log "Done"
}

main "$@"
