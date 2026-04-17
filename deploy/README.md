# 生产部署完整方案

## 目录结构

```
deploy/
├── README.md                        # 部署方案总览（本文件）
├── systemd/
│   ├── gov-backend.service          # systemd服务文件
│   ├── gov-backend.spec             # RPM打包配置
│   ├── start.sh                     # 启动脚本
│   └── gov-backend.env.example      # 环境变量模板
└── rpm/
    ├── BUILD_RPM.md                 # RPM构建指南
    └── INSTALL_RPM.md               # RPM安装指南
```

## 安装路径

所有文件安装到 `/opt/peoplesAirDefence` 目录：

```
/opt/peoplesAirDefence/
├── app.jar                          # 应用JAR
├── bin/
│   └── start.sh                     # 启动脚本
├── config/
│   ├── application.yml              # 主配置
│   ├── application-prod.yml         # 生产配置
│   ├── logback-spring.xml           # 日志配置
│   ├── gov-backend.env              # 环境变量（用户编辑）
│   ├── application.yml.default      # 默认配置
│   ├── application-prod.yml.default # 默认生产配置
│   ├── logback-spring.xml.default   # 默认日志配置
│   └── gov-backend.env.example      # 环境变量示例
└── logs/
    ├── app.log                      # 应用日志
    ├── error.log                    # 错误日志
    ├── audit.log                    # 审计日志
    └── perf.log                     # 性能日志
```

## RPM安装流程

### 1. 构建RPM包

```bash
cd gov-project-backend

# 使用Maven构建
mvn clean package rpm:rpm

# RPM包位置
ls ~/rpmbuild/RPMS/x86_64/gov-project-backend-*.rpm
```

### 2. 安装RPM包

```bash
# 首次安装
sudo rpm -ivh gov-project-backend-1.0.0-1.el7.x86_64.rpm

# 或升级
sudo rpm -Uvh gov-project-backend-1.0.0-1.el7.x86_64.rpm
```

### 3. RPM安装后的自动行为

安装完成后，RPM会**自动执行**以下操作：

#### 自动创建
- ✓ 创建服务用户 `gov-backend`
- ✓ 创建应用目录 `/opt/peoplesAirDefence`
- ✓ 创建配置目录 `/opt/peoplesAirDefence/config`
- ✓ 创建日志目录 `/opt/peoplesAirDefence/logs`
- ✓ 复制JAR文件到 `/opt/peoplesAirDefence/app.jar`
- ✓ 复制启动脚本到 `/opt/peoplesAirDefence/bin/start.sh`
- ✓ 复制systemd服务文件到 `/usr/lib/systemd/system/gov-backend.service`

#### 自动生成配置文件（首次安装）
- ✓ `/opt/peoplesAirDefence/config/application.yml` （从.default复制）
- ✓ `/opt/peoplesAirDefence/config/application-prod.yml` （从.default复制）
- ✓ `/opt/peoplesAirDefence/config/logback-spring.xml` （从.default复制）
- ✓ `/opt/peoplesAirDefence/config/gov-backend.env` （从.example复制）

#### 自动设置权限
- ✓ 所有文件所有者设为 `gov-backend:gov-backend`
- ✓ 环境变量文件权限设为 `600`（仅所有者可读写）
- ✓ 其他文件权限设为 `644`

#### 自动重新加载systemd
- ✓ 执行 `systemctl daemon-reload`

#### **不自动启动**
- ✗ 不自动启动服务
- ✗ 不自动加入开机启动

### 4. 安装后需要手动操作

#### 步骤1：配置环境变量

```bash
# 编辑环境变量文件
sudo vi /opt/peoplesAirDefence/config/gov-backend.env

# 必须修改的项：
GOV_SM4_KEY=your-production-key-16-chars
GOV_DB_PASSWORD=your-db-password
GOV_MINIO_ACCESS_KEY=your-minio-key
GOV_MINIO_SECRET_KEY=your-minio-secret

# 可选修改的项：
GOV_DB_URL=jdbc:mariadb://your-db-server:3306/gov_db...
GOV_MINIO_ENDPOINT=http://your-minio-server:9000
GOV_MINIO_PUBLIC_ENDPOINT=http://your-public-ip:port
JAVA_OPTS=-Xms2g -Xmx8g -Dfile.encoding=UTF-8
```

#### 步骤2：验证配置

```bash
# 检查环境变量文件
sudo cat /opt/peoplesAirDefence/config/gov-backend.env

# 检查权限
ls -la /opt/peoplesAirDefence/
ls -la /opt/peoplesAirDefence/config/
ls -la /opt/peoplesAirDefence/logs/
```

#### 步骤3：启动服务

```bash
# 启动服务
sudo systemctl start gov-backend

# 查看启动状态
sudo systemctl status gov-backend

# 查看启动日志
sudo journalctl -u gov-backend -f
```

#### 步骤4：加入开机启动

```bash
# 设置开机自启
sudo systemctl enable gov-backend

# 验证开机启动已启用
sudo systemctl is-enabled gov-backend
# 输出：enabled
```

## systemd服务管理

### 基本命令

```bash
# 启动服务
sudo systemctl start gov-backend

# 停止服务
sudo systemctl stop gov-backend

# 重启服务
sudo systemctl restart gov-backend

# 重新加载配置（不中断服务）
sudo systemctl reload gov-backend

# 查看服务状态
sudo systemctl status gov-backend

# 查看是否开机自启
sudo systemctl is-enabled gov-backend

# 启用开机自启
sudo systemctl enable gov-backend

# 禁用开机自启
sudo systemctl disable gov-backend
```

### 日志查看

```bash
# 实时日志（systemd日志）
sudo journalctl -u gov-backend -f

# 最近100行
sudo journalctl -u gov-backend -n 100

# 按时间范围查看
sudo journalctl -u gov-backend --since "2 hours ago"

# 应用日志文件
sudo tail -f /opt/peoplesAirDefence/logs/app.log

# 错误日志
sudo tail -f /opt/peoplesAirDefence/logs/error.log

# 审计日志
sudo tail -f /opt/peoplesAirDefence/logs/audit.log
```

## 配置管理

### 首次安装

```bash
sudo rpm -ivh gov-project-backend-1.0.0-1.el7.x86_64.rpm

# 自动生成的文件：
# /opt/peoplesAirDefence/config/gov-backend.env
# /opt/peoplesAirDefence/config/application.yml
# /opt/peoplesAirDefence/config/application-prod.yml
# /opt/peoplesAirDefence/config/logback-spring.xml
```

### 升级更新

```bash
# 升级RPM包（保留用户修改的配置）
sudo rpm -Uvh gov-project-backend-1.0.1-1.el7.x86_64.rpm

# 结果：
# - /opt/peoplesAirDefence/app.jar 更新
# - /opt/peoplesAirDefence/bin/start.sh 更新
# - /opt/peoplesAirDefence/config/*.default 更新
# - /opt/peoplesAirDefence/config/gov-backend.env 保留（用户修改）
# - /opt/peoplesAirDefence/config/application-prod.yml 保留（用户修改）
# - /opt/peoplesAirDefence/config/logback-spring.xml 保留（用户修改）
```

### 查看配置变更

```bash
# 查看新版本的默认配置
diff /opt/peoplesAirDefence/config/application-prod.yml /opt/peoplesAirDefence/config/application-prod.yml.default

# 如果需要应用新的默认配置
sudo cp /opt/peoplesAirDefence/config/application-prod.yml.default /opt/peoplesAirDefence/config/application-prod.yml
sudo chown gov-backend:gov-backend /opt/peoplesAirDefence/config/application-prod.yml
sudo systemctl restart gov-backend
```

## 常见操作

### 更新环境变量

```bash
# 编辑环境变量
sudo vi /opt/peoplesAirDefence/config/gov-backend.env

# 重启服务应用新配置
sudo systemctl restart gov-backend

# 验证
sudo systemctl status gov-backend
```

### 更新应用配置

```bash
# 编辑生产配置
sudo vi /opt/peoplesAirDefence/config/application-prod.yml

# 重启服务
sudo systemctl restart gov-backend

# 查看日志确认配置已加载
sudo journalctl -u gov-backend -n 50
```

### 回滚到上一个版本

**降级安装** 是指从新版本回到旧版本。

**为什么需要降级？**
- 新版本有bug，需要回到稳定版本
- 新版本不兼容，需要回到旧版本
- 测试新版本后决定不用

**降级步骤：**

```bash
# 1. 查看已安装的版本
rpm -qa | grep gov-project-backend
# 输出：gov-project-backend-1.0.1-1.el7.x86_64

# 2. 获取旧版本的RPM包
# 从备份或仓库中获取 gov-project-backend-1.0.0-1.el7.x86_64.rpm

# 3. 停止服务（可选，但建议）
sudo systemctl stop gov-backend

# 4. 降级安装（使用 --oldpackage 参数强制允许降级）
sudo rpm -Uvh --oldpackage gov-project-backend-1.0.0-1.el7.x86_64.rpm

# 5. 重启服务
sudo systemctl start gov-backend

# 6. 验证版本
rpm -qa | grep gov-project-backend
# 输出：gov-project-backend-1.0.0-1.el7.x86_64

# 7. 查看日志确认启动正常
sudo journalctl -u gov-backend -f
```

**降级后的结果：**
- ✓ 应用代码回到旧版本
- ✓ 配置文件保留（不会被覆盖）
- ✓ 日志文件保留

**重要提示：**
- 降级可能有风险，如果新版本做了数据库迁移，降级后可能导致数据不兼容
- 降级前建议备份数据库和配置文件
- 降级后如果出现问题，可以再升级回新版本

**备份配置和数据：**

```bash
# 备份配置文件
sudo tar czf /backup/gov-backend-config-$(date +%Y%m%d-%H%M%S).tar.gz /opt/peoplesAirDefence/config/

# 备份数据库
mysqldump -h localhost -u db_user -p gov_db > /backup/gov_db-$(date +%Y%m%d-%H%M%S).sql

# 备份日志
sudo tar czf /backup/gov-backend-logs-$(date +%Y%m%d-%H%M%S).tar.gz /opt/peoplesAirDefence/logs/
```

### 恢复默认配置

```bash
# 备份当前配置
sudo cp /opt/peoplesAirDefence/config/application-prod.yml /opt/peoplesAirDefence/config/application-prod.yml.backup

# 恢复默认配置
sudo cp /opt/peoplesAirDefence/config/application-prod.yml.default /opt/peoplesAirDefence/config/application-prod.yml

# 重启服务
sudo systemctl restart gov-backend
```

## 故障排查

### 服务启动失败

```bash
# 查看详细日志
sudo journalctl -u gov-backend -n 200

# 检查环境变量
sudo cat /opt/peoplesAirDefence/config/gov-backend.env

# 检查权限
ls -la /opt/peoplesAirDefence/
ls -la /opt/peoplesAirDefence/logs/

# 检查端口占用
sudo netstat -tlnp | grep 8080

# 检查JDK
java -version
```

### 配置未生效

```bash
# 检查service文件
sudo systemctl cat gov-backend

# 检查环境变量文件
sudo cat /opt/peoplesAirDefence/config/gov-backend.env

# 重新加载systemd
sudo systemctl daemon-reload

# 重启服务
sudo systemctl restart gov-backend
```

### 数据库连接失败

```bash
# 测试数据库连接
mysql -h db-server -u db_user -p -D gov_db

# 检查环境变量中的数据库配置
sudo grep GOV_DB /opt/peoplesAirDefence/config/gov-backend.env

# 查看应用日志
sudo tail -f /opt/peoplesAirDefence/logs/app.log
```

### MinIO连接失败

```bash
# 测试MinIO连接
curl http://minio-server:9000/minio/health/live

# 检查环境变量中的MinIO配置
sudo grep GOV_MINIO /opt/peoplesAirDefence/config/gov-backend.env

# 查看应用日志
sudo tail -f /opt/peoplesAirDefence/logs/app.log
```

## 卸载

### 卸载RPM包

```bash
# 卸载
sudo rpm -e gov-project-backend

# 验证
rpm -qa | grep gov-project-backend
```

### 清理文件（可选）

```bash
# 手动删除应用目录
sudo rm -rf /opt/peoplesAirDefence/

# 手动删除systemd服务文件
sudo rm -f /usr/lib/systemd/system/gov-backend.service

# 重新加载systemd
sudo systemctl daemon-reload
```

## 安全检查清单

部署前必须验证：

- [ ] SM4密钥已更换为生产值（16字符）
- [ ] 数据库密码已设置
- [ ] MinIO访问密钥已更换
- [ ] `GOV_SM4_ALLOW_PLAINTEXT_FALLBACK=false`
- [ ] `GOV_CSRF_ENABLED=true`
- [ ] `GOV_RATE_LIMIT_ENABLED=true`
- [ ] `GOV_API_DOCS_ENABLED=false`
- [ ] Cookie Secure标志已启用（HTTPS环境）
- [ ] 日志级别设为INFO
- [ ] 监控端口仅内网访问
- [ ] 日志目录权限正确
- [ ] systemd服务已启用开机自启
- [ ] 防火墙规则已配置
- [ ] SELinux策略已配置（如启用）

## 监控和维护

### 定期检查

```bash
# 检查服务状态
sudo systemctl status gov-backend

# 检查日志大小
du -sh /opt/peoplesAirDefence/logs/

# 检查磁盘空间
df -h /opt/

# 检查内存使用
free -h
```

### 日志轮转

日志自动轮转配置在 `logback-spring.xml` 中：
- 最大文件大小：20MB
- 保留天数：30天
- 总体积上限：2GB

### 定期备份

```bash
# 定期备份配置
sudo tar czf /backup/gov-backend-config-$(date +%Y%m%d).tar.gz /opt/peoplesAirDefence/config/

# 定期备份日志
sudo tar czf /backup/gov-backend-logs-$(date +%Y%m%d).tar.gz /opt/peoplesAirDefence/logs/
```

## 相关文档

- [RPM构建指南](rpm/BUILD_RPM.md)
- [RPM安装指南](rpm/INSTALL_RPM.md)
