# 高可用优化进度 - gov-project-backend

## 优化目标
支撑 100 人并发访问，数据高可用，数据出错快速恢复，服务快速恢复。

## 完成状态：全部完成 ✅

---

## 已完成改动清单

### 1. application.yml — HikariCP 连接池 + 优雅停机 + 限流配置
- 补齐 HikariCP 连接池参数（默认 10 → 最大 20，最小 5）
- 开启 `server.shutdown: graceful`，重启时等待请求处理完毕（30s）
- Tomcat 线程池显式配置（max 200，min-spare 20）
- 开启 Spring Actuator 健康端点（独立端口 8081）
- Sa-Token 改为允许并发登录（`is-concurrent: true`）
- 新增限流配置项（`gov.rate-limit.*`，支持环境变量覆盖）
- 新增慢 SQL 阈值配置（`gov.logging.slow-sql-ms: 500`）

### 2. AsyncTaskConfig.java — 审计线程池扩容
- 核心线程数 2 → 4，最大线程数 4 → 8
- 队列容量 2000 → 5000
- 优雅停机等待时间 10s → 30s

### 3. MybatisPlusConfig.java — 慢 SQL 拦截器
- 新增 `SlowSqlInterceptor`，超过 500ms 的 SQL 输出 warn 日志到 `perf.log`
- SQL 超长时截断到 500 字符，防止日志爆炸

### 4. HealthController.java（新增）
- 路径：`com/gov/common/controller/HealthController.java`
- `/health/live`：存活探针，进程在即返回 UP
- `/health/ready`：就绪探针，同时检查数据库连通性
- 适用于 Docker/K8s 探针、负载均衡健康检查、运维巡检

### 5. SaTokenConfig.java — 放行健康检查接口
- 新增 `/health/live` 和 `/health/ready` 到白名单，无需登录即可访问

### 6. SchemaPatchRunner.java — 补丁失败告警
- `safeExec` 和 `ensureIndex` 失败时改为 `log.warn` 输出，不再静默忽略
- 索引创建成功时输出 `log.info`，便于确认补丁执行情况
- 全部补丁执行完毕后输出完成日志

### 7. RateLimitFilter.java（新增）
- 路径：`com/gov/common/filter/RateLimitFilter.java`
- 基于令牌桶算法，按 IP 限流
- 默认每 IP 每秒 30 次，突发上限 60 次（环境变量可调）
- 超限返回 HTTP 429，JSON 格式错误提示
- 健康检查接口不参与限流
- 过滤器优先级高于审计过滤器

### 8. scripts/db_backup.py（新增）
- 使用 `mysqldump + gzip` 压缩备份
- 支持立即执行、定时模式（每天 02:30）、仅清理三种模式
- 自动清理超过保留天数的旧备份（默认 7 天）
- 所有参数通过环境变量配置，与 application.yml 保持一致

### 9. scripts/health_check.py（新增）
- 调用 `/health/ready` 接口检查服务状态
- 支持单次检查、持续巡检（`--watch N`）、自动重启（`--restart`）
- 连续失败 3 次触发 systemctl 重启
- 支持 Webhook 告警推送

---

## 环境变量速查

| 变量 | 默认值 | 说明 |
|------|--------|------|
| GOV_DB_POOL_MAX | 20 | 数据库连接池最大连接数 |
| GOV_DB_POOL_MIN | 5 | 数据库连接池最小空闲连接数 |
| GOV_RATE_LIMIT_ENABLED | true | 是否开启限流 |
| GOV_RATE_LIMIT_RPS | 30 | 每 IP 每秒最大请求数 |
| GOV_RATE_LIMIT_BURST | 60 | 每 IP 突发上限 |
| GOV_ACTUATOR_PORT | 8081 | Actuator 健康端点端口 |
| GOV_BACKUP_DIR | ./backups | 备份文件目录 |
| GOV_BACKUP_KEEP_DAYS | 7 | 备份保留天数 |
| GOV_HEALTH_URL | http://127.0.0.1:8080/api/health/ready | 健康检查地址 |
| GOV_ALERT_WEBHOOK | （空） | 告警 Webhook URL |

---

## 未覆盖（需运维层面处理）

| 项目 | 建议方案 |
|------|----------|
| MariaDB 单点 | 配置主从复制，从库做读备份 |
| MinIO 单点 | 升级为多节点或使用对象存储服务 |
| 进程守护 | Docker `restart: always` 或 systemd |
| Nginx 前置 | 静态资源缓存 + gzip + 反向代理 |
| 监控告警 | 接入 Prometheus + Grafana 或简单脚本巡检 |
