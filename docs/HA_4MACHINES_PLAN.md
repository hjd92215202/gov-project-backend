# 四机器高可用方案

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
         ┌───────────────┼───────────────┬───────────────┐
         │               │               │               │
    ┌────▼────┐     ┌────▼────┐    ┌────▼────┐    ┌────▼────┐
    │ 后端应用1 │     │ 后端应用2 │    │ 后端应用3 │    │ 后端应用4 │
    │ 8080     │     │ 8080     │    │ 8080     │    │ 8080     │
    │ 机器1    │     │ 机器2    │    │ 机器3    │    │ 机器4    │
    └────┬────┘     └────┬────┘    └────┬────┘    └────┬────┘
         │               │               │               │
         └───────────────┼───────────────┼───────────────┘
                         │
         ┌───────────────┼───────────────┬───────────────┐
         │               │               │               │
    ┌────▼────┐     ┌────▼────┐    ┌────▼────┐    ┌────▼────┐
    │ MinIO    │     │ MariaDB  │    │ MariaDB  │    │ MinIO    │
    │ 节点1    │     │ 主库     │    │ 从库     │    │ 节点4    │
    │ 9000     │     │ 3306     │    │ 3306     │    │ 9000     │
    │ 机器1    │     │ 机器2    │    │ 机器3    │    │ 机器4    │
    └─────────┘     └────┬────┘    └────┬────┘    └─────────┘
                         │               │
                    ┌────▼────┐     ┌────▼────┐
                    │ MinIO    │     │ MinIO    │
                    │ 节点2    │     │ 节点3    │
                    │ 9000     │     │ 9000     │
                    │ 机器2    │     │ 机器3    │
                    └─────────┘     └─────────┘
```

## 机器分配方案

### 机器1：Nginx 负载均衡 + 后端应用 + MinIO 节点1
```
配置：2核 4GB / 50GB SSD
角色：
  - Nginx 反向代理（80/443）
  - 后端应用实例1（8080）
  - MinIO 节点1（9000/9001）
  - 健康检查脚本
```

### 机器2：后端应用 + MariaDB 主库 + MinIO 节点2
```
配置：4核 8GB / 100GB SSD + 500GB HDD
角色：
  - 后端应用实例2（8080）
  - MariaDB 主库（3306）
  - MinIO 节点2（9000/9001）
  - 数据库备份脚本
```

### 机器3：后端应用 + MariaDB 从库 + MinIO 节点3
```
配置：4核 8GB / 100GB SSD + 500GB HDD
角色：
  - 后端应用实例3（8080）
  - MariaDB 从库（3306）- 只读
  - MinIO 节点3（9000/9001）
```

### 机器4：后端应用 + MinIO 节点4
```
配置：4核 8GB / 100GB SSD + 500GB HDD
角色：
  - 后端应用实例4（8080）
  - MinIO 节点4（9000/9001）
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
# 所有机器
sudo firewall-cmd --permanent --add-port=80/tcp
sudo firewall-cmd --permanent --add-port=443/tcp
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --permanent --add-port=8081/tcp
sudo firewall-cmd --permanent --add-port=3306/tcp
sudo firewall-cmd --permanent --add-port=9000/tcp
sudo firewall-cmd --permanent --add-port=9001/tcp
sudo firewall-cmd --reload
```

#### 1.3 hosts 配置
```bash
# 所有机器的 /etc/hosts
192.168.1.10  machine1  nginx-lb-minio1
192.168.1.11  machine2  backend-db-master-minio2
192.168.1.12  machine3  backend-db-slave-minio3
192.168.1.13  machine4  backend-minio4
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

# 安装 curl
sudo yum install -y curl
```

---

### 第三步：MariaDB 主从配置

#### 3.1 机器2 - MariaDB 主库

```bash
mkdir -p /data/mariadb/master/conf
mkdir -p /data/mariadb/init

cat > /data/mariadb/master/conf/master.cnf << 'EOF'
[mysqld]
server-id=1
log-bin=mysql-bin
binlog-format=ROW
binlog-row-image=FULL
max_binlog_size=100M
expire_logs_days=7
EOF

cat > docker-compose.yml << 'EOF'
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
EOF

docker-compose up -d

# 创建复制用户
docker exec mariadb-master mysql -uroot -proot@123 << 'EOF'
CREATE USER 'repl'@'%' IDENTIFIED BY 'repl@123';
GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';
FLUSH PRIVILEGES;
SHOW MASTER STATUS;
EOF
```

记录 `File` 和 `Position`。

#### 3.2 机器3 - MariaDB 从库

```bash
mkdir -p /data/mariadb/slave/conf

cat > /data/mariadb/slave/conf/slave.cnf << 'EOF'
[mysqld]
server-id=2
relay-log=relay-bin
read-only=ON
skip-slave-start
EOF

cat > docker-compose.yml << 'EOF'
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
EOF

docker-compose up -d

# 配置主从复制
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

---

### 第四步：MinIO 分布式集群部署（所有机器）

#### 4.1 创建共享存储目录

```bash
# 所有机器执行
mkdir -p /data/minio
```

#### 4.2 机器1 - MinIO 节点1

```bash
cat > docker-compose.yml << 'EOF'
version: '3.8'
services:
  minio:
    image: minio/minio:latest
    container_name: minio
    restart: always
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: govadmin
      MINIO_ROOT_PASSWORD: govadminpassword
    volumes:
      - /data/minio:/minio_data
    command: >
      server
      http://192.168.1.10:9000/minio_data
      http://192.168.1.11:9000/minio_data
      http://192.168.1.12:9000/minio_data
      http://192.168.1.13:9000/minio_data
      --console-address ":9001"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 30s
      timeout: 20s
      retries: 3
EOF

docker-compose up -d
```

#### 4.3 机器2、3、4 - MinIO 节点2、3、4

```bash
# 机器2、3、4 执行相同命令（只改 container_name）
cat > docker-compose.yml << 'EOF'
version: '3.8'
services:
  minio:
    image: minio/minio:latest
    container_name: minio
    restart: always
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: govadmin
      MINIO_ROOT_PASSWORD: govadminpassword
    volumes:
      - /data/minio:/minio_data
    command: >
      server
      http://192.168.1.10:9000/minio_data
      http://192.168.1.11:9000/minio_data
      http://192.168.1.12:9000/minio_data
      http://192.168.1.13:9000/minio_data
      --console-address ":9001"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 30s
      timeout: 20s
      retries: 3
EOF

docker-compose up -d
```

#### 4.4 创建 MinIO 存储桶

```bash
# 在任意一台机器上执行
curl https://dl.min.io/client/mc/release/linux-amd64/mc -o /usr/local/bin/mc
chmod +x /usr/local/bin/mc

mc alias set minio http://192.168.1.10:9000 govadmin govadminpassword

mc mb minio/gov-files
mc policy set public minio/gov-files
```

---

### 第五步：后端应用部署（所有机器）

#### 5.1 准备应用包

```bash
cd gov-project-backend
powershell -ExecutionPolicy Bypass -File scripts\package-kylin-arm.ps1
```

将 `app.jar` 上传到四台机器。

#### 5.2 机器1 - 后端应用1

```bash
mkdir -p /opt/gov-backend
cd /opt/gov-backend

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
GOV_MINIO_ENDPOINT=http://192.168.1.10:9000
GOV_MINIO_PUBLIC_ENDPOINT=http://192.168.1.10:9000
GOV_MINIO_ACCESS_KEY=govadmin
GOV_MINIO_SECRET_KEY=govadminpassword
GOV_MINIO_BUCKET_NAME=gov-files
EOF

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

#### 5.3 机器2、3、4 - 后端应用2、3、4

```bash
# 机器2、3、4 执行相同步骤，只改 GOV_DB_URL 和 container_name
# 机器2：GOV_DB_URL=jdbc:mariadb://localhost:3306/...
# 机器3、4：GOV_DB_URL=jdbc:mariadb://192.168.1.11:3306/...
```

---

### 第六步：Nginx 负载均衡配置（机器1）

```bash
mkdir -p /etc/nginx/conf.d

cat > /etc/nginx/conf.d/gov-backend.conf << 'EOF'
upstream gov_backend {
    least_conn;
    server 192.168.1.10:8080 max_fails=3 fail_timeout=30s;
    server 192.168.1.11:8080 max_fails=3 fail_timeout=30s;
    server 192.168.1.12:8080 max_fails=3 fail_timeout=30s;
    server 192.168.1.13:8080 max_fails=3 fail_timeout=30s;
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

docker run -d \
  --name nginx-lb \
  --restart always \
  -p 80:80 \
  -p 443:443 \
  -v /etc/nginx/conf.d:/etc/nginx/conf.d \
  nginx:latest

# 验证
docker exec nginx-lb nginx -t
```

---

### 第七步：数据库备份配置（机器2）

```bash
mkdir -p /opt/backup

cat > /opt/backup/db_backup.sh << 'EOF'
#!/bin/bash

BACKUP_DIR="/data/backups"
KEEP_DAYS=7
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/gov_db_$TIMESTAMP.sql.gz"

mkdir -p $BACKUP_DIR

docker exec mariadb-master mysqldump \
  -udb_user -pEgov@123 \
  --single-transaction \
  --quick \
  --lock-tables=false \
  gov_db | gzip > $BACKUP_FILE

if [ $? -eq 0 ]; then
    echo "[$(date)] 备份成功: $BACKUP_FILE"
    find $BACKUP_DIR -name "gov_db_*.sql.gz" -mtime +$KEEP_DAYS -delete
else
    echo "[$(date)] 备份失败"
    exit 1
fi
EOF

chmod +x /opt/backup/db_backup.sh

# 设置定时备份
crontab -e
# 添加：30 2 * * * /opt/backup/db_backup.sh >> /var/log/gov_backup.log 2>&1
```

---

### 第八步：健康检查与监控（机器1）

```bash
mkdir -p /opt/health

cat > /opt/health/check.sh << 'EOF'
#!/bin/bash

BACKEND_URLS=(
    "http://192.168.1.10:8080/api/health/live"
    "http://192.168.1.11:8080/api/health/live"
    "http://192.168.1.12:8080/api/health/live"
    "http://192.168.1.13:8080/api/health/live"
)

MINIO_URLS=(
    "http://192.168.1.10:9000/minio/health/live"
    "http://192.168.1.11:9000/minio/health/live"
    "http://192.168.1.12:9000/minio/health/live"
    "http://192.168.1.13:9000/minio/health/live"
)

echo "[$(date)] ===== 后端健康检查 ====="
for url in "${BACKEND_URLS[@]}"; do
    if curl -sf "$url" > /dev/null 2>&1; then
        echo "[$(date)] ✓ $url"
    else
        echo "[$(date)] ✗ $url"
    fi
done

echo "[$(date)] ===== MinIO 健康检查 ====="
for url in "${MINIO_URLS[@]}"; do
    if curl -sf "$url" > /dev/null 2>&1; then
        echo "[$(date)] ✓ $url"
    else
        echo "[$(date)] ✗ $url"
    fi
done

# 检查 MariaDB 主从状态
echo "[$(date)] ===== MariaDB 主从状态 ====="
docker exec mariadb-slave mysql -uroot -proot@123 -e "SHOW SLAVE STATUS\G" | grep -E "Slave_IO_Running|Slave_SQL_Running"
EOF

chmod +x /opt/health/check.sh

# 设置定时检查
crontab -e
# 添加：*/5 * * * * /opt/health/check.sh >> /var/log/gov_health.log 2>&1
```

---

## MinIO 集群特性

### 纠删码配置
```
4 个节点 → 2+2 纠删码
- 2 个数据块 + 2 个校验块
- 可以容忍 2 个节点同时故障
- 数据可靠性：99.99%
```

### 故障转移
```
场景1：1 个 MinIO 节点故障
→ 集群自动降级，继续服务
→ 数据仍可恢复

场景2：2 个 MinIO 节点故障
→ 集群继续服务
→ 数据仍可恢复

场景3：3 个 MinIO 节点故障
→ 集群不可用
→ 需要手动恢复
```

### 性能
```
写入：4 个节点并行写入，性能提升 4 倍
读取：可以从任意节点读取，性能提升 4 倍
```

---

## 故障恢复流程

### 场景1：某个后端实例故障

```bash
# Nginx 自动转移流量
# Docker 自动重启容器
docker restart gov-backend-1
```

### 场景2：MariaDB 主库故障

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

### 场景3：MinIO 节点故障

```bash
# MinIO 集群自动处理，无需手动干预
# 检查集群状态
mc admin info minio
```

---

## 监控指标

| 指标 | 告警阈值 | 检查频率 |
|------|----------|----------|
| 后端实例存活 | 任何实例宕机 | 10s |
| MinIO 集群健康 | 任何节点宕机 | 30s |
| 数据库主从延迟 | >5s | 30s |
| 磁盘使用率 | >80% | 1min |
| 内存使用率 | >85% | 1min |
| 数据库连接数 | >15 | 1min |

---

## 成本估算

| 项目 | 配置 | 月成本 |
|------|------|--------|
| 机器1 | 2核 4GB | ¥200 |
| 机器2 | 4核 8GB | ¥400 |
| 机器3 | 4核 8GB | ¥400 |
| 机器4 | 4核 8GB | ¥400 |
| 带宽 | 5Mbps | ¥100 |
| **总计** | | **¥1500** |

---

## 部署时间表

| 阶段 | 任务 | 预计时间 |
|------|------|----------|
| 1 | 网络配置 + 基础软件 | 1 小时 |
| 2 | MariaDB 主从配置 | 1 小时 |
| 3 | MinIO 集群部署 | 1 小时 |
| 4 | 后端应用部署 | 1 小时 |
| 5 | Nginx 配置 | 30 分钟 |
| 6 | 备份 + 监控配置 | 1 小时 |
| 7 | 测试 + 验证 | 1 小时 |
| **总计** | | **6.5 小时** |

---

## 验证清单

部署完成后，逐项验证：

- [ ] 四台机器网络连通（ping 互相）
- [ ] MariaDB 主从同步正常（`SHOW SLAVE STATUS`）
- [ ] MinIO 集群正常（`mc admin info minio`）
- [ ] 四个后端实例都能启动（`docker ps`）
- [ ] Nginx 能访问所有后端（`curl http://localhost/api/health/live`）
- [ ] 数据库备份脚本能执行
- [ ] 健康检查脚本能执行
- [ ] 模拟一个后端实例故障，验证自动转移
- [ ] 模拟一个 MinIO 节点故障，验证集群继续服务
- [ ] 模拟两个 MinIO 节点故障，验证数据可恢复

---

## 后续优化

1. **接入 Prometheus + Grafana**（可视化监控）
2. **配置 SSL/TLS**（HTTPS）
3. **添加 Redis 缓存**（提升性能）
4. **迁移到 Kubernetes**（自动扩缩容）
5. **配置 CDN**（加速静态资源）
