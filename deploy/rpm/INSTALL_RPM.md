# RPM包安装指南

## 前置条件

- CentOS 7+ 或 RHEL 7+
- JDK 1.8 已安装
- MariaDB 10.11+ 已安装或容器化运行
- MinIO 已安装或容器化运行
- 网络连接正常

## 安装步骤

### 1. 获取RPM包

```bash
# 从构建服务器复制
scp user@build-server:/path/to/gov-project-backend-1.0.0-1.el7.x86_64.rpm .

# 或从仓库安装
sudo yum install gov-project-backend
```

### 2. 安装RPM包

```bash
# 首次安装
sudo rpm -ivh gov-project-backend-1.0.0-1.el7.x86_64.rpm

# 或升级
sudo rpm -Uvh gov-project-backend-1.0.0-1.el7.x86_64.rpm
```

### 3. 验证安装

```bash
# 查看已安装的包
rpm -qa | grep gov-project-backend

# 查看包内容
rpm -ql gov-project-backend

# 查看包信息
rpm -qi gov-project-backend
```

### 4. 配置环境变量

```bash
# 编辑环境变量文件
sudo vi /etc/gov-backend/gov-backend.env

# 填入生产值
GOV_SM4_KEY=your-production-key-16-chars
GOV_PASSWORD_PEPPER=your-pepper-value
GOV_DB_URL=jdbc:mariadb://db-server:3306/gov_db?useUnicode=true&characterEncoding=utf8mb4&connectionCollation=utf8mb4_unicode_ci&serverTimezone=GMT%2B8&useSSL=false&allowPublicKeyRetrieval=true&useInformationSchema=true
GOV_DB_USERNAME=db_user
GOV_DB_PASSWORD=your-db-password
GOV_MINIO_ENDPOINT=http://minio-server:9000
GOV_MINIO_PUBLIC_ENDPOINT=http://minio.prod.example.com
GOV_MINIO_ACCESS_KEY=your-minio-key
GOV_MINIO_SECRET_KEY=your-minio-secret
JAVA_OPTS=-Xms2g -Xmx8g -Dfile.encoding=UTF-8
```

### 5. 验证配置

```bash
# 检查环境变量文件
sudo cat /etc/gov-backend/gov-backend.env

# 检查权限
ls -la /etc/gov-backend/
ls -la /opt/gov-backend/
ls -la /var/log/gov-backend/
```

### 6. 启动服务

```bash
# 启动服务
sudo systemctl start gov-backend

# 设置开机自启
sudo systemctl enable gov-backend

# 查看状态
sudo systemctl status gov-backend
```

### 7. 验证启动

```bash
# 查看日志
sudo journalctl -u gov-backend -f

# 检查端口
sudo netstat -tlnp | grep 8080

# 测试API
curl http://localhost:8080/api/actuator/health
```

## 服务管理

### 基本命令

```bash
# 启动
sudo systemctl start gov-backend

# 停止
sudo systemctl stop gov-backend

# 重启
sudo systemctl restart gov-backend

# 重新加载配置
sudo systemctl reload gov-backend

# 查看状态
sudo systemctl status gov-backend

# 查看启用状态
sudo systemctl is-enabled gov-backend
```

### 日志查看

```bash
# 实时日志
sudo journalctl -u gov-backend -f

# 最近100行
sudo journalctl -u gov-backend -n 100

# 按时间范围查看
sudo journalctl -u gov-backend --since "2 hours ago"

# 应用日志文件
sudo tail -f /var/log/gov-backend/app.log

# 错误日志
sudo tail -f /var/log/gov-backend/error.log

# 审计日志
sudo tail -f /var/log/gov-backend/audit.log
```

## 配置更新

### 更新环境变量

```bash
# 编辑环境变量
sudo vi /etc/gov-backend/gov-backend.env

# 重启服务应用新配置
sudo systemctl restart gov-backend

# 验证
sudo systemctl status gov-backend
```

### 更新应用配置

```bash
# 编辑生产配置
sudo vi /etc/gov-backend/application-prod.yml

# 重启服务
sudo systemctl restart gov-backend

# 查看日志确认配置已加载
sudo journalctl -u gov-backend -n 50
```

### 查看配置变更

```bash
# 查看新版本的默认配置
diff /etc/gov-backend/application-prod.yml /etc/gov-backend/application-prod.yml.default

# 如果需要应用新的默认配置
sudo cp /etc/gov-backend/application-prod.yml.default /etc/gov-backend/application-prod.yml
sudo chown gov-backend:gov-backend /etc/gov-backend/application-prod.yml
sudo systemctl restart gov-backend
```

## 升级更新

### 升级RPM包

```bash
# 升级（保留用户修改的配置）
sudo rpm -Uvh gov-project-backend-1.0.1-1.el7.x86_64.rpm

# 结果：
# - /opt/gov-backend/app.jar 更新
# - /opt/gov-backend/bin/start.sh 更新
# - /etc/gov-backend/*.default 更新
# - /etc/gov-backend/gov-backend.env 保留
# - /etc/gov-backend/application-prod.yml 保留
```

### 升级后验证

```bash
# 查看新版本
rpm -qi gov-project-backend

# 检查配置变更
diff /etc/gov-backend/application-prod.yml /etc/gov-backend/application-prod.yml.default

# 重启服务
sudo systemctl restart gov-backend

# 查看日志
sudo journalctl -u gov-backend -f
```

## 回滚操作

### 回滚到上一个版本

```bash
# 查看已安装的版本
rpm -qa | grep gov-project-backend

# 降级安装
sudo rpm -Uvh --oldpackage gov-project-backend-1.0.0-1.el7.x86_64.rpm

# 重启服务
sudo systemctl restart gov-backend
```

### 恢复配置

```bash
# 备份当前配置
sudo cp /etc/gov-backend/application-prod.yml /etc/gov-backend/application-prod.yml.backup

# 恢复默认配置
sudo cp /etc/gov-backend/application-prod.yml.default /etc/gov-backend/application-prod.yml

# 重启服务
sudo systemctl restart gov-backend
```

## 卸载

### 卸载RPM包

```bash
# 卸载
sudo rpm -e gov-project-backend

# 验证
rpm -qa | grep gov-project-backend
```

### 清理配置文件

```bash
# 手动删除配置目录（可选）
sudo rm -rf /etc/gov-backend/

# 手动删除日志目录（可选）
sudo rm -rf /var/log/gov-backend/

# 手动删除应用目录（可选）
sudo rm -rf /opt/gov-backend/
```

## 故障排查

### 服务启动失败

```bash
# 查看详细日志
sudo journalctl -u gov-backend -n 200

# 检查环境变量
sudo cat /etc/gov-backend/gov-backend.env

# 检查权限
ls -la /opt/gov-backend/
ls -la /var/log/gov-backend/

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
sudo cat /etc/gov-backend/gov-backend.env

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
sudo grep GOV_DB /etc/gov-backend/gov-backend.env

# 查看应用日志
sudo tail -f /var/log/gov-backend/app.log
```

### MinIO连接失败

```bash
# 测试MinIO连接
curl http://minio-server:9000/minio/health/live

# 检查环境变量中的MinIO配置
sudo grep GOV_MINIO /etc/gov-backend/gov-backend.env

# 查看应用日志
sudo tail -f /var/log/gov-backend/app.log
```

## 监控和维护

### 定期检查

```bash
# 检查服务状态
sudo systemctl status gov-backend

# 检查日志大小
du -sh /var/log/gov-backend/

# 检查磁盘空间
df -h /var/log/

# 检查内存使用
free -h
```

### 日志轮转

日志自动轮转配置在 `logback-spring.xml` 中：
- 最大文件大小：20MB
- 保留天数：30天
- 总体积上限：2GB

### 备份配置

```bash
# 定期备份配置
sudo tar czf /backup/gov-backend-config-$(date +%Y%m%d).tar.gz /etc/gov-backend/

# 定期备份日志
sudo tar czf /backup/gov-backend-logs-$(date +%Y%m%d).tar.gz /var/log/gov-backend/
```

## 安全检查清单

- [ ] SM4密钥已更换为生产值
- [ ] 数据库密码已设置
- [ ] MinIO访问密钥已更换
- [ ] `GOV_SM4_ALLOW_PLAINTEXT_FALLBACK=false`
- [ ] `GOV_CSRF_ENABLED=true`
- [ ] `GOV_RATE_LIMIT_ENABLED=true`
- [ ] `GOV_API_DOCS_ENABLED=false`
- [ ] Cookie Secure标志已启用
- [ ] 日志级别设为INFO
- [ ] 监控端口仅内网访问
- [ ] 日志目录权限正确
- [ ] systemd服务已启用开机自启
- [ ] 防火墙规则已配置
- [ ] SELinux策略已配置（如启用）
