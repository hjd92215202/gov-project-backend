# Deployment Overview (ARM Default)

## Directory Layout

```text
deploy/
├── README.md
├── systemd/
│   ├── gov-backend.service
│   ├── gov-backend.spec
│   ├── start.sh
│   ├── rpm-hooks.sh
│   ├── extrafilelist.txt
│   └── gov-backend.env.example
└── rpm/
    ├── BUILD_RPM.md
    └── INSTALL_RPM.md
```

## Packaging Strategy

- Default target architecture: `aarch64`
- RPM build mode: prebuilt binary package (`%build` does not compile source)
- Install root: `/opt/peoplesAirDefence`
- Service file: `/lib/systemd/system/gov-backend.service`
- Install policy: `systemctl enable` on install, but no automatic `start`

## Installed Paths

```text
/opt/peoplesAirDefence/
├── app.jar
├── bin/
│   ├── start.sh
│   └── rpm-hooks.sh
├── etc/
│   ├── application.yml.default
│   ├── application-prod.yml.default
│   ├── logback-spring.xml.default
│   ├── gov-backend.env.example
│   ├── application.yml                 # created if absent during post-install
│   ├── application-prod.yml            # created if absent during post-install
│   ├── logback-spring.xml              # created if absent during post-install
│   └── gov-backend.env                 # created if absent during post-install
├── doc/
│   └── extrafilelist.txt
├── data/
└── logs/
```

## Build and Install

- Build guide: [rpm/BUILD_RPM.md](rpm/BUILD_RPM.md)
- Install guide: [rpm/INSTALL_RPM.md](rpm/INSTALL_RPM.md)

## First-Start Bootstrap Notes

- On first startup against a fresh DB, bootstrap initializes baseline RBAC data.
- Admin initial password is encoded with current `GOV_PASSWORD_PEPPER`.
- Admin reset runs only on first initialization, not every restart.
