# ARM RPM Install Guide

## Prerequisites

- ARM Linux (`aarch64`)
- JDK 8 installed
- MariaDB and MinIO reachable
- `systemd` available

## 1) Install RPM

```bash
sudo rpm -ivh gov-project-backend-1.0.0-1.aarch64.rpm
```

Upgrade:

```bash
sudo rpm -Uvh gov-project-backend-1.0.1-1.aarch64.rpm
```

## 2) Verify Install

```bash
rpm -qa | grep gov-project-backend
rpm -qi gov-project-backend
rpm -ql gov-project-backend
```

## 3) Configure Environment

Edit:

```bash
sudo vi /opt/peoplesAirDefence/etc/gov-backend.env
```

At minimum update:

- `GOV_SM4_KEY`
- `GOV_DB_URL` / `GOV_DB_USERNAME` / `GOV_DB_PASSWORD`
- `GOV_MINIO_ENDPOINT` / `GOV_MINIO_ACCESS_KEY` / `GOV_MINIO_SECRET_KEY`

## 4) Service Enablement Strategy

After install:

- `systemctl is-enabled gov-backend` should be `enabled`
- service is **not auto-started** by rpm

Check:

```bash
sudo systemctl is-enabled gov-backend
sudo systemctl is-active gov-backend
```

Expected:

- `enabled`
- `inactive`

## 5) Start Service Manually

```bash
sudo systemctl start gov-backend
sudo systemctl status gov-backend
sudo journalctl -u gov-backend -f
```

## 6) Runtime Paths

- App root: `/opt/peoplesAirDefence`
- Config: `/opt/peoplesAirDefence/etc`
- Logs: `/opt/peoplesAirDefence/logs`
- Service file: `/lib/systemd/system/gov-backend.service`
- Dynamic file list: `/opt/peoplesAirDefence/doc/extrafilelist.txt`

## 7) Uninstall

```bash
sudo rpm -e gov-project-backend
```

The package does not delete business data or remove service user aggressively.
