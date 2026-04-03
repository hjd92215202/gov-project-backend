# 三机器高可用方案

## 架构设计

```
┌──────────────────────────────────────────────────────────┐
│                    互联网 / 用户                          │
└────────────────────────┬─────────────────────────────────┘
                         │
                    ┌────▼────┐
                    │ Nginx LB │  (机器1)
                    │ 80/443   │  2核 4GB
                    └────┬────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
    ┌────▼────┐     ┌────▼────┐    ┌────▼────┐
    │ 后端应用1 │     │ 后端应用2 │    │ 后端应用3 │
    │ 8080     │     │ 8080     │    │ 8080     │
    │ 机器1    │     │ 机器2    │    │ 机器3    │
    └────┬────┘     └────┬────┘    └────┬────┘
         │               │               │
         └───────────────┼───────────────┘
                         │
                    ┌────▼────┐
                    │ MariaDB  │  (机器2)
                    │ 主库     │  8核 16GB
                    │ 3306     │
                    └────┬────┘
                         │
                    ┌────▼────┐
                    │ MariaDB  │  (机器3)
                    │ 从库     │  8核 16GB
                    │ 3306     │
                    └─────────┘
```

## 机器分配方案

### 机器1：Nginx 负载均衡 + 后端应用
```
配置：2核 4GB / 50GB SSD
角色：
  - Nginx 反向代理（80/443）
  - 后端应用实例1（8080）
  - 健康检查脚本
```

### 机器2：后端应用 + MariaDB 主库
```
配置：4核 8GB / 100GB SSD + 500GB HDD
角色：
  - 后端应用实例2（8080）
  - MariaDB 主库（3306）
  - 数据库备份脚本
```

### 机器3：后端应用 + MariaDB 从库
```
配置：4核 8GB / 100GB SSD + 500GB HDD
角色：
  - 后端应用实例3（8080）
  - MariaDB 从库（3306）
  - 从库只读，用于读备份
```

---

## 部署步骤

### 第一步：网络配置（所有机器）

#### 1.1 时间同步
```bash
# 所有机器执行
sudo timedatectl set-timezone Asia/Shanghai
sudo systemctl enable systemd-timesyncd
sudo systemctl start systemd-timesyncd
```

#### 1.2 防火墙规则
```bash
# 机器1（Nginx）
sudo firewall-cmd --permanent --add-port=80/tcp
sudo firewall-cmd --permanent --add-port=443/tcp
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload

# 机器2（后端 + MariaDB 主）
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --permanent --add-port=8081/tcp
sudo firewall-cmd --permanent --add-port=3306/tcp
sudo firewall-cmd --reload

# 机器3（后端 + MariaDB 从）
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --permanent --add-port=8081/tcp
sudo firewall-cmd --permanent --add-port=3306/tcp
sudo firewall-cmd --reload
```

#### 1.3 hosts 配置（便于服务发现）
```bash
# 所有机器的 /etc/hosts
192.168.1.10  machine1  nginx-lb
192.168.1.11  machine2  backend-db-master
192.168.1.12  machine3  backend-db-slave
```

---

### 第二步：基础软件安装（所有机器）

```bash
# 更新系统
sudo yum update -y

# 安装 Docker
sudo yum install -y docker
sudo systemctl enable docker
sudo systemctl start docker

# 安装 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 安装 Git
sudo yum install -y git

# 安装 curl（用于健康检查）
sudo yum install -y curl
```

---

### 第三步：MariaDB 主从配置

#### 3.1 机器2 - MariaDB 主库

创建 `docker-compose.yml`：
```yaml
version: '3.8'
services:
  mariadb-master:
    image: mariadb:10.5
    container_name: mariadb-master
    restart: always
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: root@123
      MYSQL_DATABASE: gov_db
      MYSQL_USER: db_user
      MYSQL_PASSWORD: Egov@123
    volumes:
      - /data/mariadb/master:/var/lib/mysql
      - /data/mariadb/master/conf:/etc/mysql/conf.d
      - /data/mariadb/init:/docker-entrypoint-initdb.d
    command: >
      --server-id=1
      --log-bin=mysql-bin
      --binlog-format=ROW
      --binlog-row-image=FULL
      --max_binlog_size=100M
      --expire_logs_days=7
```

启动主库：
```bash
mkdir -p /data/mariadb/master/conf
mkdir -p /data/mariadb/init

# 创建主库配置文件
cat > /data/mariadb/master/conf/master.cnf << 'EOF'
[mysqld]
server-id=1
log-bin=mysql-bin
binlog-format=ROW
binlog-row-image=FULL
max_binlog_size=100M
expire_logs_days=7
EOF

docker-compose up -d
```

创建复制用户：
```bash
docker exec mariadb-master mysql -uroot -proot@123 << 'EOF'
CREATE USER 'repl'@'%' IDENTIFIED BY 'repl@123';
GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';
FLUSH PRIVILEGES;
SHOW MASTER STATUS;
EOF
```

记录输出的 `File` 和 `Position`（后面配置从库需要用）。

#### 3.2 机器3 - MariaDB 从库

创建 `docker-compose.yml`：
```yaml
version: '3.8'
services:
  mariadb-slave:
    image: mariadb:10.5
    container_name: mariadb-slave
    restart: always
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: root@123
      MYSQL_DATABASE: gov_db
      MYSQL_USER: db_user
      MYSQL_PASSWORD: Egov@123
    volumes:
      - /data/mariadb/slave:/var/lib/mysql
      - /data/mariadb/slave/conf:/etc/mysql/conf.d
    command: >
      --server-id=2
      --relay-log=relay-bin
      --read-only=ON
      --skip-slave-start
```

启动从库：
```bash
mkdir -p /data/mariadb/slave/conf

cat > /data/mariadb/slave/conf/slave.cnf << 'EOF'
[mysqld]
server-id=2
relay-log=relay-bin
read-only=ON
skip-slave-start
EOF

docker-compose up -d
```

配置主从复制（替换 MASTER_LOG_FILE 和 MASTER_LOG_POS）：
```bash
docker exec mariadb-slave mysql -uroot -proot@123 << 'EOF'
CHANGE MASTER TO
  MASTER_HOST='192.168.1.11',
  MASTER_USER='repl',
  MASTER_PASSWORD='repl@123',
  MASTER_LOG_FILE='mysql-bin.000001',
  MASTER_LOG_POS=154;

START SLAVE;
SHOW SLAVE STATUS\G
EOF
```

验证从库状态：
```bash
docker exec mariadb-slave mysql -uroot -proot@123 -e "SHOW SLAVE STATUS\G"
```

确保 `Slave_IO_Running: Yes` 和 `Slave_SQL_Running: Yes`。

---

### 第四步：后端应用部署（所有机器）

#### 4.1 准备应用包

在本地打包：
```bash
cd gov-project-backend
powershell -ExecutionPolicy Bypass -File scripts\package-kylin-arm.ps1
```

将 `deploy-output/gov4/backend/app.jar` 上传到三台机器。

#### 4.2 机器1 - 后端应用1

```bash
mkdir -p /opt/gov-backend
cd /opt/gov-backend

# 上传 app.jar
scp app.jar root@192.168.1.10:/opt/gov-backend/

# 创建 backend.env
cat > backend.env << 'EOF'
GOV_DB_URL=jdbc:mariadb://192.168.1.11:3306/gov_db?useUnicode=true&characterEncoding=utf8mb4&connectionCollation=utf8mb4_unicode_ci&serverTimezone=GMT%2B8&useSSL=false&allowPublicKeyRetrieval=true&useInformationSchema=true
GOV_DB_USERNAME=db_user
GOV_DB_PASSWORD=Egov@123
GOV_DB_POOL_MAX=20
GOV_DB_POOL_MIN=5
GOV_RATE_LIMIT_ENABLED=true
GOV_RATE_LIMIT_RPS=30
GOV_RATE_LIMIT_BURST=60
GOV_ACTUATOR_PORT=8081
EOF

# 创建 docker-compose.yml
cat > docker-compose.yml << 'EOF'
version: '3.8'
services:
  backend:
    image: openjdk:8-jre-slim
    container_name: gov-backend-1
    restart: always
    ports:
      - "8080:8080"
      - "8081:8081"
    environment:
      JAVA_OPTS: "-Xmx2g -Xms1g"
    volumes:
      - /opt/gov-backend/app.jar:/app/app.jar
      - /opt/gov-backend/logs:/app/logs
    working_dir: /app
    command: java -jar app.jar
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/health/live"]
      interval: 10s
      timeout: 5s
      retries: 3
EOF

docker-compose up -d
```

#### 4.3 机器2 - 后端应用2

```bash
mkdir -p /opt/gov-backend
cd /opt/gov-backend

# 创建 backend.env（数据库指向本机主库）
cat > backend.env << 'EOF'
GOV_DB_URL=jdbc:mariadb://localhost:3306/gov_db?useUnicode=true&characterEncoding=utf8mb4&connectionCollation=utf8mb4_unicode_ci&serverTimezone=GMT%2B8&useSSL=false&allowPublicKeyRetrieval=true&useInformationSchema=true
GOV_DB_USERNAME=db_user
GOV_DB_PASSWORD=Egov@123
GOV_DB_POOL_MAX=20
GOV_DB_POOL_MIN=5
GOV_RATE_LIMIT_ENABLED=true
GOV_RATE_LIMIT_RPS=30
GOV_RATE_LIMIT_BURST=60
GOV_ACTUATOR_PORT=8081
EOF

# docker-compose.yml 同上
docker-compose up -d
```

#### 4.4 机器3 - 后端应用3

```bash
mkdir -p /opt/gov-backend
cd /opt/gov-backend

# 创建 backend.env（数据库指向本机从库，但应用仍然写主库）
cat > backend.env << 'EOF'
GOV_DB_URL=jdbc:mariadb://192.168.1.11:3306/gov_db?useUnicode=true&characterEncoding=utf8mb4&connectionCollation=utf8mb4_unicode_ci&serverTimezone=GMT%2B8&useSSL=false&allowPublicKeyRetrieval=true&useInformationSchema=true
GOV_DB_USERNAME=db_user
GOV_DB_PASSWORD=Egov@123
GOV_DB_POOL_MAX=20
GOV_DB_POOL_MIN=5
GOV_RATE_LIMIT_ENABLED=true
GOV_RATE_LIMIT_RPS=30
GOV_RATE_LIMIT_BURST=60
GOV_ACTUATOR_PORT=8081
EOF

docker-compose up -d
```

---

### 第五步：Nginx 负载均衡配置（机器1）

```bash
mkdir -p /etc/nginx/conf.d

cat > /etc/nginx/conf.d/gov-backend.conf << 'EOF'
upstream gov_backend {
    least_conn;
    server 192.168.1.10:8080 max_fails=3 fail_timeout=30s;
    server 192.168.1.11:8080 max_fails=3 fail_timeout=30s;
    server 192.168.1.12:8080 max_fails=3 fail_timeout=30s;
}

server {
    listen 80;
    server_name _;
    client_max_body_size 500M;

    location /api {
        proxy_pass http://gov_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 30s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;
        proxy_buffering off;
    }

    location /health {
        access_log off;
        proxy_pass http://gov_backend;
    }
}
EOF

# 启动 Nginx
docker run -d \
  --name nginx-lb \
  --restart always \
  -p 80:80 \
  -p 443:443 \
  -v /etc/nginx/conf.d:/etc/nginx/conf.d \
  nginx:latest
```

验证 Nginx 配置：
```bash
docker exec nginx-lb nginx -t
```

---

### 第六步：数据库备份配置（机器2）

创建备份脚本 `/opt/backup/db_backup.sh`：

```bash
#!/bin/bash

BACKUP_DIR="/data/backups"
KEEP_DAYS=7
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/gov_db_$TIMESTAMP.sql.gz"

mkdir -p $BACKUP_DIR

# 执行备份
docker exec mariadb-master mysqldump \
  -udb_user -pEgov@123 \
  --single-transaction \
  --quick \
  --lock-tables=false \
  gov_db | gzip > $BACKUP_FILE

if [ $? -eq 0 ]; then
    echo "[$(date)] 备份成功: $BACKUP_FILE"
    
    # 清理旧备份
    find $BACKUP_DIR -name "gov_db_*.sql.gz" -mtime +$KEEP_DAYS -delete
else
    echo "[$(date)] 备份失败"
    exit 1
fi
```

设置定时备份（每天 02:30）：
```bash
chmod +x /opt/backup/db_backup.sh

# 编辑 crontab
crontab -e

# 添加以下行
30 2 * * * /opt/backup/db_backup.sh >> /var/log/gov_backup.log 2>&1
```

---

### 第七步：健康检查与自动恢复（机器1）

创建健康检查脚本 `/opt/health/check.sh`：

```bash
#!/bin/bash

BACKEND_URLS=(
    "http://192.168.1.10:8080/api/health/live"
    "http://192.168.1.11:8080/api/health/live"
    "http://192.168.1.12:8080/api/health/live"
)

FAILED_COUNT=0
MAX_FAILURES=3

for url in "${BACKEND_URLS[@]}"; do
    if ! curl -sf "$url" > /dev/null 2>&1; then
        echo "[$(date)] 健康检查失败: $url"
        FAILED_COUNT=$((FAILED_COUNT + 1))
    fi
done

if [ $FAILED_COUNT -gt 0 ]; then
    echo "[$(date)] 有 $FAILED_COUNT 个后端实例不健康"
fi
```

设置定时检查（每 5 分钟）：
```bash
chmod +x /opt/health/check.sh

crontab -e

# 添加以下行
*/5 * * * * /opt/health/check.sh >> /var/log/gov_health.log 2>&1
```

---

## 故障恢复流程

### 场景1：某个后端实例故障

**自动处理**：
- Nginx 检测到实例不健康，自动转移流量到其他实例
- 该实例的 Docker 容器配置了 `restart: always`，自动重启

**手动恢复**：
```bash
# 在故障机器上
docker restart gov-backend-1
```

### 场景2：MariaDB 主库故障

**手动切换**：
```bash
# 在从库机器（机器3）上
docker exec mariadb-slave mysql -uroot -proot@123 << 'EOF'
STOP SLAVE;
RESET MASTER;
SET GLOBAL read_only=OFF;
EOF

# 更新后端应用连接字符串，指向新主库（192.168.1.12）
# 重启后端应用
```

### 场景3：Nginx 故障

**手动恢复**：
```bash
# 在机器1上
docker restart nginx-lb

# 或者在机器2/3上临时启动 Nginx
docker run -d --name nginx-lb-backup -p 80:80 nginx:latest
```

---

## 监控指标

### 关键指标

| 指标 | 告警阈值 | 检查频率 |
|------|----------|----------|
| 后端实例存活 | 任何实例宕机 | 10s |
| 数据库主从延迟 | >5s | 30s |
| 磁盘使用率 | >80% | 1min |
| 内存使用率 | >85% | 1min |
| 数据库连接数 | >15 | 1min |

### 查看监控

```bash
# 查看后端实例状态
curl http://192.168.1.10/api/health/ready

# 查看数据库主从状态
docker exec mariadb-slave mysql -uroot -proot@123 -e "SHOW SLAVE STATUS\G"

# 查看 Nginx 状态
docker exec nginx-lb nginx -s status

# 查看磁盘使用
df -h

# 查看内存使用
free -h
```

---

## 成本估算

| 项目 | 配置 | 月成本 |
|------|------|--------|
| 机器1 | 2核 4GB | ¥200 |
| 机器2 | 4核 8GB | ¥400 |
| 机器3 | 4核 8GB | ¥400 |
| 带宽 | 5Mbps | ¥100 |
| **总计** | | **¥1100** |

---

## 部署时间表

| 阶段 | 任务 | 预计时间 |
|------|------|----------|
| 1 | 网络配置 + 基础软件 | 1 小时 |
| 2 | MariaDB 主从配置 | 1 小时 |
| 3 | 后端应用部署 | 1 小时 |
| 4 | Nginx 配置 | 30 分钟 |
| 5 | 备份 + 监控配置 | 1 小时 |
| 6 | 测试 + 验证 | 1 小时 |
| **总计** | | **5.5 小时** |

---

## 验证清单

部署完成后，逐项验证：

- [ ] 三台机器网络连通（ping 互相）
- [ ] MariaDB 主从同步正常（`SHOW SLAVE STATUS`）
- [ ] 三个后端实例都能启动（`docker ps`）
- [ ] Nginx 能访问所有后端（`curl http://localhost/api/health/live`）
- [ ] 数据库备份脚本能执行
- [ ] 健康检查脚本能执行
- [ ] 模拟一个后端实例故障，验证自动转移
- [ ] 模拟数据库主库故障，验证从库可读

---

## 后续优化

1. **添加 MinIO 对象存储**（如果需要文件存储）
2. **接入 Prometheus + Grafana**（可视化监控）
3. **配置 SSL/TLS**（HTTPS）
4. **添加 Redis 缓存**（提升性能）
5. **迁移到 Kubernetes**（自动扩缩容）
