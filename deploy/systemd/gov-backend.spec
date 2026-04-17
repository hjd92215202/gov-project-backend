Name:           gov-project-backend
Version:        1.0.0
Release:        1%{?dist}
Summary:        Government Project Backend Service
License:        Proprietary

%description
Government Project Backend Service - Spring Boot Application

%prep
# 源代码已在构建前准备

%build
cd %{_builddir}/%{name}-%{version}
mvn clean package -DskipTests

%install
# 创建目录结构
mkdir -p %{buildroot}/opt/peoplesAirDefence/bin
mkdir -p %{buildroot}/opt/peoplesAirDefence/config
mkdir -p %{buildroot}/opt/peoplesAirDefence/logs
mkdir -p %{buildroot}/usr/lib/systemd/system

# 复制JAR文件
cp %{_builddir}/%{name}-%{version}/target/gov-project-backend-*.jar %{buildroot}/opt/peoplesAirDefence/app.jar

# 复制启动脚本
cp %{_builddir}/%{name}-%{version}/deploy/systemd/start.sh %{buildroot}/opt/peoplesAirDefence/bin/
chmod +x %{buildroot}/opt/peoplesAirDefence/bin/start.sh

# 复制systemd service文件
cp %{_builddir}/%{name}-%{version}/deploy/systemd/gov-backend.service %{buildroot}/usr/lib/systemd/system/

# 复制配置文件（标记为config，支持更新时保留用户修改）
cp %{_builddir}/%{name}-%{version}/src/main/resources/application.yml %{buildroot}/opt/peoplesAirDefence/config/application.yml.default
cp %{_builddir}/%{name}-%{version}/src/main/resources/application-prod.yml %{buildroot}/opt/peoplesAirDefence/config/application-prod.yml.default
cp %{_builddir}/%{name}-%{version}/src/main/resources/logback-spring.xml %{buildroot}/opt/peoplesAirDefence/config/logback-spring.xml.default
cp %{_builddir}/%{name}-%{version}/deploy/systemd/gov-backend.env.example %{buildroot}/opt/peoplesAirDefence/config/gov-backend.env.example

%files
/opt/peoplesAirDefence/app.jar
/opt/peoplesAirDefence/bin/start.sh
/usr/lib/systemd/system/gov-backend.service
%config(noreplace) /opt/peoplesAirDefence/config/application.yml.default
%config(noreplace) /opt/peoplesAirDefence/config/application-prod.yml.default
%config(noreplace) /opt/peoplesAirDefence/config/logback-spring.xml.default
/opt/peoplesAirDefence/config/gov-backend.env.example
%dir /opt/peoplesAirDefence/logs

%post
# 创建服务用户
useradd -r -s /bin/false gov-backend 2>/dev/null || true

# 设置权限
chown -R gov-backend:gov-backend /opt/peoplesAirDefence
chmod -R 755 /opt/peoplesAirDefence

# 首次安装时复制配置文件
if [ ! -f /opt/peoplesAirDefence/config/application.yml ]; then
    cp /opt/peoplesAirDefence/config/application.yml.default /opt/peoplesAirDefence/config/application.yml
    chown gov-backend:gov-backend /opt/peoplesAirDefence/config/application.yml
    chmod 644 /opt/peoplesAirDefence/config/application.yml
fi

if [ ! -f /opt/peoplesAirDefence/config/application-prod.yml ]; then
    cp /opt/peoplesAirDefence/config/application-prod.yml.default /opt/peoplesAirDefence/config/application-prod.yml
    chown gov-backend:gov-backend /opt/peoplesAirDefence/config/application-prod.yml
    chmod 644 /opt/peoplesAirDefence/config/application-prod.yml
fi

if [ ! -f /opt/peoplesAirDefence/config/logback-spring.xml ]; then
    cp /opt/peoplesAirDefence/config/logback-spring.xml.default /opt/peoplesAirDefence/config/logback-spring.xml
    chown gov-backend:gov-backend /opt/peoplesAirDefence/config/logback-spring.xml
    chmod 644 /opt/peoplesAirDefence/config/logback-spring.xml
fi

if [ ! -f /opt/peoplesAirDefence/config/gov-backend.env ]; then
    cp /opt/peoplesAirDefence/config/gov-backend.env.example /opt/peoplesAirDefence/config/gov-backend.env
    chown gov-backend:gov-backend /opt/peoplesAirDefence/config/gov-backend.env
    chmod 600 /opt/peoplesAirDefence/config/gov-backend.env
fi

# 重新加载systemd
systemctl daemon-reload

echo "Installation complete. To start the service:"
echo "  1. Edit /opt/peoplesAirDefence/config/gov-backend.env with your production values"
echo "  2. systemctl start gov-backend"
echo "  3. systemctl enable gov-backend"

%preun
# 停止服务
systemctl stop gov-backend 2>/dev/null || true
systemctl disable gov-backend 2>/dev/null || true

%postun
# 删除服务用户
userdel gov-backend 2>/dev/null || true

%changelog
* Mon Apr 17 2026 Admin <admin@example.com> - 1.0.0-1
- Initial release
