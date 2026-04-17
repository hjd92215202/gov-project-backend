Name:           gov-project-backend
Version:        %{?pkg_version}%{!?pkg_version:1.0.0}
Release:        %{?pkg_release}%{!?pkg_release:1}%{?dist}
Summary:        Government Project Backend Service
License:        Proprietary
BuildArch:      aarch64
Source0:        %{name}-%{version}.tar.gz

Requires(post): /bin/bash
Requires(post): systemd
Requires(preun): /bin/bash
Requires(preun): systemd
Requires(postun): /bin/bash
Requires(postun): systemd

%description
Government Project Backend Service - Spring Boot Application

%prep
%setup -q

%build
# Prebuilt binary package. No source compilation in RPM build stage.

%install
rm -rf %{buildroot}

install -d %{buildroot}/opt/peoplesAirDefence/bin
install -d %{buildroot}/opt/peoplesAirDefence/etc
install -d %{buildroot}/opt/peoplesAirDefence/doc
install -d %{buildroot}/opt/peoplesAirDefence/data
install -d %{buildroot}/opt/peoplesAirDefence/logs
install -d %{buildroot}/lib/systemd/system

install -m 0644 target/gov-project-backend-%{version}.jar %{buildroot}/opt/peoplesAirDefence/app.jar
install -m 0755 deploy/systemd/start.sh %{buildroot}/opt/peoplesAirDefence/bin/start.sh
install -m 0755 deploy/systemd/rpm-hooks.sh %{buildroot}/opt/peoplesAirDefence/bin/rpm-hooks.sh
install -m 0644 deploy/systemd/gov-backend.service %{buildroot}/lib/systemd/system/gov-backend.service

install -m 0644 src/main/resources/application.yml %{buildroot}/opt/peoplesAirDefence/etc/application.yml.default
install -m 0644 src/main/resources/application-prod.yml %{buildroot}/opt/peoplesAirDefence/etc/application-prod.yml.default
install -m 0644 src/main/resources/logback-spring.xml %{buildroot}/opt/peoplesAirDefence/etc/logback-spring.xml.default
install -m 0644 deploy/systemd/gov-backend.env.example %{buildroot}/opt/peoplesAirDefence/etc/gov-backend.env.example
install -m 0644 deploy/systemd/extrafilelist.txt %{buildroot}/opt/peoplesAirDefence/doc/extrafilelist.txt

%files
/opt/peoplesAirDefence/app.jar
/opt/peoplesAirDefence/bin/start.sh
/opt/peoplesAirDefence/bin/rpm-hooks.sh
/lib/systemd/system/gov-backend.service
%config(noreplace) /opt/peoplesAirDefence/etc/application.yml.default
%config(noreplace) /opt/peoplesAirDefence/etc/application-prod.yml.default
%config(noreplace) /opt/peoplesAirDefence/etc/logback-spring.xml.default
%config(noreplace) /opt/peoplesAirDefence/etc/gov-backend.env.example
%config(noreplace) /opt/peoplesAirDefence/doc/extrafilelist.txt
%dir /opt/peoplesAirDefence/data
%dir /opt/peoplesAirDefence/logs

%post
/bin/bash /opt/peoplesAirDefence/bin/rpm-hooks.sh post "$1" || true

%preun
if [ -x /opt/peoplesAirDefence/bin/rpm-hooks.sh ]; then
    /bin/bash /opt/peoplesAirDefence/bin/rpm-hooks.sh preun "$1" || true
fi

%postun
if [ -x /opt/peoplesAirDefence/bin/rpm-hooks.sh ]; then
    /bin/bash /opt/peoplesAirDefence/bin/rpm-hooks.sh postun "$1" || true
else
    systemctl daemon-reload >/dev/null 2>&1 || true
fi

%changelog
* Fri Apr 17 2026 Admin <admin@example.com> - 1.0.0-1
- ARM-first RPM packaging with external lifecycle hooks
