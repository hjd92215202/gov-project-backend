# RPM包构建指南

## 前置条件

- CentOS 7+ 或 RHEL 7+
- Maven 3.6+
- JDK 1.8
- rpmbuild工具（可选）

## 方式1：使用Maven插件（推荐）

### 1. 配置pom.xml

在 `pom.xml` 中添加rpm-maven-plugin：

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>rpm-maven-plugin</artifactId>
    <version>2.3.0</version>
    <configuration>
        <name>gov-project-backend</name>
        <version>1.0.0</version>
        <release>1</release>
        <targetOS>linux</targetOS>
        <requires>
            <require>java-1.8.0-openjdk</require>
        </requires>
    </configuration>
</plugin>
```

### 2. 构建RPM

```bash
cd gov-project-backend
mvn clean package rpm:rpm
```

### 3. 查找RPM包

```bash
# RPM包位置
ls ~/rpmbuild/RPMS/x86_64/gov-project-backend-*.rpm

# 或
find . -name "*.rpm" -type f
```

## 方式2：使用rpmbuild工具

### 1. 安装rpmbuild

```bash
sudo yum install -y rpm-build
```

### 2. 准备构建目录

```bash
mkdir -p ~/rpmbuild/{BUILD,RPMS,SOURCES,SPECS,SRPMS,tmp}
```

### 3. 准备源代码

```bash
cd gov-project-backend
tar czf ~/rpmbuild/SOURCES/gov-project-backend-1.0.0.tar.gz .
```

### 4. 复制spec文件

```bash
cp deploy/systemd/gov-backend.spec ~/rpmbuild/SPECS/
```

### 5. 构建RPM

```bash
rpmbuild -bb ~/rpmbuild/SPECS/gov-backend.spec
```

### 6. 查找RPM包

```bash
ls ~/rpmbuild/RPMS/x86_64/gov-project-backend-*.rpm
```

## 版本管理

### 更新版本号

1. **编辑pom.xml**
```xml
<version>1.0.1</version>
```

2. **编辑deploy/systemd/gov-backend.spec**
```
Version:        1.0.1
Release:        1
```

### 构建新版本

```bash
mvn clean package rpm:rpm
```

## RPM包内容

```
gov-project-backend-1.0.0-1.el7.x86_64.rpm
├── /opt/gov-backend/
│   ├── app.jar
│   └── bin/start.sh
├── /etc/gov-backend/
│   ├── application.yml.default
│   ├── application-prod.yml.default
│   ├── logback-spring.xml.default
│   └── gov-backend.env.example
├── /usr/lib/systemd/system/
│   └── gov-backend.service
└── /var/log/gov-backend/
```

## 安装后的文件

首次安装时，RPM会自动生成：

```bash
/etc/gov-backend/
├── application.yml              # 从.default复制
├── application-prod.yml         # 从.default复制
├── logback-spring.xml           # 从.default复制
├── gov-backend.env              # 从.example复制
├── application.yml.default      # 原始默认配置
├── application-prod.yml.default # 原始默认配置
├── logback-spring.xml.default   # 原始默认配置
└── gov-backend.env.example      # 环境变量示例
```

## 故障排查

### 构建失败：找不到JDK

```bash
# 检查JDK版本
java -version

# 设置JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk
```

### 构建失败：Maven命令不存在

```bash
# 安装Maven
sudo yum install -y maven

# 或使用项目自带的Maven wrapper
./mvnw clean package rpm:rpm
```

### RPM包过大

```bash
# 检查JAR包大小
ls -lh target/gov-project-backend-*.jar

# 可以在pom.xml中配置排除不必要的依赖
```

## 发布到仓库

### 本地yum仓库

```bash
# 复制RPM到仓库目录
sudo cp ~/rpmbuild/RPMS/x86_64/gov-project-backend-*.rpm /var/www/html/repo/

# 更新仓库元数据
sudo createrepo /var/www/html/repo/

# 在目标服务器上安装
sudo yum install gov-project-backend
```

### 私有仓库（Artifactory、Nexus等）

```bash
# 上传到Artifactory
curl -u user:password -T gov-project-backend-1.0.0-1.el7.x86_64.rpm \
    http://artifactory.example.com/artifactory/rpm-repo/

# 在目标服务器上配置仓库
sudo vi /etc/yum.repos.d/gov-backend.repo

# 内容：
[gov-backend]
name=Government Backend Repository
baseurl=http://artifactory.example.com/artifactory/rpm-repo/
enabled=1
gpgcheck=0

# 安装
sudo yum install gov-project-backend
```

## 最佳实践

1. **版本控制** - 在Git中跟踪版本号变更
2. **构建验证** - 构建后验证RPM包内容
3. **测试安装** - 在测试环境先验证RPM安装
4. **灰度发布** - 先在少数服务器上验证
5. **备份配置** - 升级前备份 `/etc/gov-backend/`
6. **监控告警** - 监控服务启动状态和日志
