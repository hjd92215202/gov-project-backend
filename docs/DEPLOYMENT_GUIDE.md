# 一键打包部署指南

## 概述

本指南说明如何使用 `package-kylin-arm.ps1` 脚本进行一键打包，生成生产部署包。该脚本自动化了前后端构建、文件组织和部署配置的全过程。

## 前置条件

### 系统要求
- Windows 10 Pro 或更高版本
- PowerShell 5.0 或更高版本

### 软件依赖
- **JDK 1.8** - 后端编译和运行
  - 安装路径示例：`C:\Program Files\Java\jdk1.8.0_112`
  - 环境变量：`JAVA_HOME` 需正确配置
  
- **Maven 3.6+** - 后端项目构建
  - 安装路径示例：`C:\Program Files\Apache\maven-3.6.3`
  - 环境变量：`MAVEN_HOME` 需正确配置
  
- **Node.js 14+** - 前端项目构建
  - 安装路径示例：`C:\Program Files\nodejs`
  - 验证：`node --version` 和 `npm --version`
  
- **Docker & Docker Compose** - 容器化部署
  - 用于运行 MariaDB、MinIO、后端服务、前端服务

## 打包脚本说明

### 脚本位置
```
gov-project-backend/scripts/package-kylin-arm.ps1
```

### 脚本参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `$FrontendRepo` | 前端项目路径 | 后端项目父目录下的 `gov-web` |
| `$BackendRepo` | 后端项目路径 | 脚本所在目录的父目录 |
| `$OutputDir` | 输出目录 | `{BackendRepo}/deploy-output/gov4` |

### 使用方式

#### 方式一：使用默认参数
```powershell
cd C:\Users\brace\Documents\work\gov\gov-project-backend
powershell -ExecutionPolicy Bypass -File scripts\package-kylin-arm.ps1
```

#### 方式二：指定自定义路径
```powershell
powershell -ExecutionPolicy Bypass -File scripts\package-kylin-arm.ps1 `
  -FrontendRepo "C:\path\to\gov-web" `
  -BackendRepo "C:\path\to\gov-project-backend" `
  -OutputDir "C:\deploy-output\gov4"
```

## 打包流程

### 1. 前端构建
```
输入：gov-web 项目目录
执行：npm run build
输出：dist/ 目录（静态资源）
```

**失败处理**：如果前端构建失败，脚本会立即停止并报错。

### 2. 后端构建
```
输入：gov-project-backend 项目目录
执行：mvn clean package -DskipTests
输出：target/*.jar（可执行 JAR 包）
```

**失败处理**：如果后端构建失败，脚本会立即停止并报错。

### 3. 文件组织
脚本在 `$OutputDir` 中创建以下目录结构：

```
deploy-output/gov4/
├── backend/
│   ├── app.jar                    # 后端应用 JAR 包
│   └── backend.env                # 后端环境配置（从 backend.env.example 复制）
├── frontend/
│   ├── dist/                      # 前端静态资源
│   ├── nginx.conf                 # Nginx 配置
│   ├── frontend.env               # 前端环境配置（从 frontend.env.example 复制）
│   └── runtime/                   # 运行时目录
├── mariadb/
│   └── init/
│       └── RBAC.sql               # 数据库初始化脚本
└── scripts/
    ├── prepare-directories.sh     # 准备目录结构
    ├── run-mariadb.sh             # 启动 MariaDB
    ├── wait-mariadb-and-init.sh   # 等待 MariaDB 就绪并初始化
    ├── run-minio.sh               # 启动 MinIO
    ├── run-backend.sh             # 启动后端服务
    ├── run-frontend.sh            # 启动前端服务
    ├── inspect-runtime.sh         # 检查运行时状态
    ├── deploy-all.sh              # 一键部署脚本
    └── README.md                  # 部署说明
```

## 环境配置

### 后端环境变量 (backend.env)

```bash
# 数据库配置
GOV_DB_URL=jdbc:mariadb://mariadb:13306/gov_db?useUnicode=true&characterEncoding=utf8mb4&connectionCollation=utf8mb4_unicode_ci&serverTimezone=GMT%2B8&useSSL=false&allowPublicKeyRetrieval=true&useInformationSchema=true
GOV_DB_USERNAME=db_user
GOV_DB_PASSWORD=Egov@123
GOV_DB_DRIVER_CLASS_NAME=org.mariadb.jdbc.Driver

# 数据库连接池
GOV_DB_POOL_MAX=20
GOV_DB_POOL_MIN=5

# MinIO 配置
GOV_MINIO_ENDPOINT=http://minio:9000
GOV_MINIO_PUBLIC_ENDPOINT=http://minio:9000
GOV_MINIO_ACCESS_KEY=govadmin
GOV_MINIO_SECRET_KEY=govadminpassword
GOV_MINIO_BUCKET_NAME=gov-files

# 安全配置
GOV_SM4_KEY=1234567812345678
GOV_SM4_ENABLED=true
GOV_SM4_ALLOW_PLAINTEXT_FALLBACK=true
GOV_PASSWORD_PEPPER=

# 限流配置
GOV_RATE_LIMIT_ENABLED=true
GOV_RATE_LIMIT_RPS=30
GOV_RATE_LIMIT_BURST=60

# 执行器端口
GOV_ACTUATOR_PORT=8081
```

### 前端环境变量 (frontend.env)

```bash
# API 后端地址
REACT_APP_API_URL=http://localhost:8080/api

# 其他前端配置
REACT_APP_ENV=production
```

## 部署步骤

### 步骤 1：准备部署环境

在部署目标机器上，进入打包输出目录：

```bash
cd deploy-output/gov4
```

### 步骤 2：准备目录结构

```bash
bash scripts/prepare-directories.sh
```

此脚本会创建必要的目录和权限配置。

### 步骤 3：启动 MariaDB

```bash
bash scripts/run-mariadb.sh
```

### 步骤 4：等待 MariaDB 就绪并初始化

```bash
bash scripts/wait-mariadb-and-init.sh
```

此脚本会：
- 等待 MariaDB 服务就绪
- 执行 `mariadb/init/RBAC.sql` 初始化数据库
- 创建必要的表和初始数据

### 步骤 5：启动 MinIO

```bash
bash scripts/run-minio.sh
```

### 步骤 6：启动后端服务

编辑 `backend/backend.env` 根据实际环境调整配置，然后：

```bash
bash scripts/run-backend.sh
```

后端服务将在 `http://localhost:8080/api` 上运行。

### 步骤 7：启动前端服务

编辑 `frontend/frontend.env` 根据实际环境调整配置，然后：

```bash
bash scripts/run-frontend.sh
```

前端服务将在 `http://localhost:80` 上运行。

### 一键部署（可选）

如果所有服务都需要启动，可以使用一键部署脚本：

```bash
bash scripts/deploy-all.sh
```

此脚本会按顺序执行上述所有步骤。

## 服务验证

### 检查服务状态

```bash
bash scripts/inspect-runtime.sh
```

此脚本会显示所有运行中的容器和服务状态。

### 健康检查端点

- **后端健康检查**：`http://localhost:8080/api/health/live`
- **后端就绪检查**：`http://localhost:8080/api/health/ready`
- **执行器端点**：`http://localhost:8081/actuator/health`

### 日志查看

```bash
# 后端日志
docker logs gov-backend

# 前端日志
docker logs gov-frontend

# MariaDB 日志
docker logs gov-mariadb

# MinIO 日志
docker logs gov-minio
```

## 故障排查

### 前端构建失败

**症状**：`Frontend build failed.`

**排查步骤**：
1. 检查 Node.js 版本：`node --version`（需要 14+）
2. 检查 npm 依赖：`cd gov-web && npm install`
3. 查看详细错误：`npm run build`

### 后端构建失败

**症状**：`Backend package failed.`

**排查步骤**：
1. 检查 JDK 版本：`java -version`（需要 1.8）
2. 检查 Maven 版本：`mvn --version`（需要 3.6+）
3. 清理 Maven 缓存：`mvn clean`
4. 查看详细错误：`mvn clean package -DskipTests`

### JAR 包未找到

**症状**：`Backend jar not found in target directory.`

**排查步骤**：
1. 确认后端构建成功
2. 检查 `target/` 目录是否存在 `.jar` 文件
3. 确保没有 `original-*.jar` 文件（脚本会排除这些）

### 数据库连接失败

**症状**：后端启动失败，日志显示数据库连接错误

**排查步骤**：
1. 检查 MariaDB 容器是否运行：`docker ps | grep mariadb`
2. 检查 `backend.env` 中的数据库配置
3. 验证数据库初始化脚本是否执行：`docker exec gov-mariadb mysql -u db_user -p gov_db -e "SHOW TABLES;"`

### 端口被占用

**症状**：容器启动失败，提示端口已被占用

**排查步骤**：
1. 检查占用的端口：`netstat -ano | findstr :8080`（Windows）或 `lsof -i :8080`（Linux/Mac）
2. 停止占用端口的进程或修改配置中的端口号
3. 重新启动服务

## 性能优化建议

### 数据库连接池
- 根据并发用户数调整 `GOV_DB_POOL_MAX` 和 `GOV_DB_POOL_MIN`
- 对于 100 人并发，推荐 `MAX=20, MIN=5`

### 限流配置
- `GOV_RATE_LIMIT_RPS=30`：每秒最多 30 个请求
- `GOV_RATE_LIMIT_BURST=60`：突发容量 60 个请求
- 根据实际需求调整这些值

### 日志配置
- 生产环境建议将日志级别设置为 `INFO`
- 定期清理日志文件以节省磁盘空间

## 备份和恢复

### 数据库备份

```bash
# 使用提供的备份脚本
python scripts/db_backup.py --mode immediate --output backup.sql.gz

# 或使用 mysqldump
docker exec gov-mariadb mysqldump -u db_user -p gov_db | gzip > backup.sql.gz
```

### 数据库恢复

```bash
# 恢复备份
gunzip < backup.sql.gz | docker exec -i gov-mariadb mysql -u db_user -p gov_db
```

## 常见问题

**Q: 如何修改服务端口？**
A: 编辑相应的 `.env` 文件或部署脚本中的端口配置，然后重启服务。

**Q: 如何在生产环境中使用 HTTPS？**
A: 在 Nginx 配置中添加 SSL 证书配置，或使用反向代理（如 Traefik）处理 HTTPS。

**Q: 如何扩展到多个后端实例？**
A: 使用 Docker Compose 或 Kubernetes 编排多个后端容器，配合负载均衡器（如 Nginx）。

**Q: 如何监控服务健康状态？**
A: 使用提供的 `scripts/health_check.py` 脚本进行持续监控和自动恢复。

## 相关文档

- [HA 优化进度](./HA_OPTIMIZATION_PROGRESS.md)
- [运维手册](./OPS_RUNBOOK.md)
- [系统架构](./ARCHITECTURE.md)
