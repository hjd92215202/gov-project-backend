# ARM RPM Build Guide (Strict Spec Compliance)

## Summary

- Default target architecture is `aarch64`.
- RPM build uses a **prebuilt JAR** workflow (no source compile in `%build` stage).
- Package lifecycle logic is externalized to `rpm-hooks.sh`.
- Optional signing is supported. If GPG key is missing, script continues and marks result as `unsigned`.

## Prerequisites

- ARM Linux host (`uname -m` must be `aarch64`)
- JDK 8
- Maven 3.6+
- `rpm-build`, `rpmsign` (optional), `gpg`, `sha256sum`

## One-Click Build (Recommended)

```bash
cd /path/to/gov-project-backend
chmod +x deploy/rpm/build_arm_rpm.sh

./deploy/rpm/build_arm_rpm.sh \
  --version 1.0.0 \
  --release 1 \
  --sign false \
  --output-dir ./target/rpm-artifacts
```

### Signed Build (Optional)

```bash
./deploy/rpm/build_arm_rpm.sh \
  --version 1.0.0 \
  --release 1 \
  --sign true \
  --gpg-key-name "Your RPM Signing Key" \
  --output-dir ./target/rpm-artifacts
```

## Outputs

The script exports artifacts to `--output-dir`:

- `gov-project-backend-<version>-<release>*.aarch64.rpm`
- `*.sha256`
- `*.kv.txt` (`rpm -Kv` output)
- `manifest.txt` (`signed` or `unsigned` state included)

## Manual Verification

```bash
# Package metadata
rpm -qpi target/rpm-artifacts/gov-project-backend-*.aarch64.rpm

# Package content
rpm -qlp target/rpm-artifacts/gov-project-backend-*.aarch64.rpm

# Signature / digest verification
rpm -Kv target/rpm-artifacts/gov-project-backend-*.aarch64.rpm
```

Expected architecture:

```text
Architecture: aarch64
```

## Compliance Notes

- Install root is unified under `/opt/peoplesAirDefence`.
- Required dynamic-file interface exists at:
  - `/opt/peoplesAirDefence/doc/extrafilelist.txt`
- `systemd` strategy is:
  - install-time `enable`
  - no install-time `start`
