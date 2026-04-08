# ARM Docker 部署说明

## 镜像约定
- MariaDB: `cr.kylinos.cn/basekylin/mariadb:10.3.39-ky10sp3_2403`
- Nginx: `cr.kylinos.cn/basekylin/nginx:1.21.5-ky10sp3_2403`
- Java: `cr.kylinos.cn/basekylin/java-openjdk-8-aarch64:1.8.0-v10sp1`
- MinIO: `minio/minio:RELEASE.2023-08-09T23-30-22Z`

## 目录约定
```text
/opt/gov4
├── backend
│   ├── app.jar
│   ├── backend.env
│   └── logs
├── frontend
│   ├── dist
│   ├── frontend.env
│   ├── nginx.conf
│   └── runtime
├── mariadb
│   ├── data
│   └── init
├── minio
│   └── data
└── scripts
```

## 部署前必读
- 对外统一入口仍使用 `81` 端口，Nginx 负责代理前端页面和 `/api`。
- 业务附件访问已切换为后端受控预览/下载链路，不再依赖前端直接访问 `/minio/` 公网对象地址。
- 如保留 `/minio/` 代理，仅用于运维排查，不应把该地址返回给业务页面。
- `/opt/gov4/backend/backend.env` 必须在启动前手工填写完整；`CHANGE_ME` 仅代表占位值。
- 后端启动前会校验 `GOV_DB_PASSWORD`、`GOV_MINIO_ACCESS_KEY`、`GOV_MINIO_SECRET_KEY`、`GOV_SM4_KEY`，缺失或仍为占位值时会直接失败。
- `run-mariadb.sh`、`wait-mariadb-and-init.sh`、`run-minio.sh` 不再内置演示口令，缺失敏感配置时会直接退出。

## backend.env 必填项
- `GOV_DB_PASSWORD`
- `MYSQL_ROOT_PASSWORD`
- `GOV_MINIO_ACCESS_KEY`
- `GOV_MINIO_SECRET_KEY`
- `GOV_SM4_KEY`

## 关键安全相关环境变量
- `GOV_AUTH_COOKIE_SECURE`
  - 生产环境建议为 `true`
  - 本地纯 HTTP 调试可设为 `false`
- `GOV_API_DOCS_ENABLED`
  - 生产默认 `false`
  - 仅本地开发或受控环境需要 Swagger/Knife4j 时设为 `true`
- `GOV_TRUSTED_PROXY_SOURCES`
  - 默认留空，表示不信任 `X-Forwarded-For` / `X-Real-IP`
  - 若使用 Nginx 反代，请填写反代出口 IP，多个值用逗号分隔
- `GOV_CSRF_ENABLED`
  - 默认 `true`
  - 认证已切换为 Cookie + CSRF 双提交方案，正常部署不建议关闭
- `GOV_CSRF_COOKIE_NAME`
  - 默认 `XSRF-TOKEN`
- `GOV_CSRF_HEADER_NAME`
  - 默认 `X-CSRF-Token`

## MinIO 说明
- `GOV_MINIO_ENDPOINT` 填容器网络内地址，例如 `http://gov4-minio:9000`
- `GOV_MINIO_PUBLIC_ENDPOINT` 仅保留给运维或兼容场景，不再作为前端主链路附件地址
- MinIO 控制台仅绑定 `127.0.0.1:9001`，远程访问请使用 SSH 隧道

## 启动步骤
```bash
chmod +x /opt/gov4/scripts/*.sh
sh /opt/gov4/scripts/deploy-all.sh
```

## 检查命令
```bash
docker ps -a
docker logs --tail 200 gov4-mariadb
docker logs --tail 200 gov4-minio
docker logs --tail 200 gov4-backend
docker logs --tail 200 gov4-frontend
```

## 常见排查项
- 后端启动即退出
  - 检查 `/opt/gov4/backend/backend.env` 是否仍有 `CHANGE_ME`
  - 检查 `GOV_SM4_KEY` 是否为 16 位密钥
- 登录后写接口返回 403
  - 确认前端和后端同源访问
  - 确认浏览器已携带认证 Cookie 与 `X-CSRF-Token`
- 审计和限流 IP 不正确
  - 未配置 `GOV_TRUSTED_PROXY_SOURCES` 时，系统只信任 `request.getRemoteAddr()`
  - 使用 Nginx 反代时，需要把反代出口地址加入可信代理白名单
- 无法访问文档
  - 部署默认关闭 Swagger/Knife4j
  - 如需启用，请显式设置 `GOV_API_DOCS_ENABLED=true`

## 访问地址
- 前端首页: `http://<host>:81/`
- 后端接口前缀: `http://<host>:81/api`
- MinIO 控制台: `http://127.0.0.1:9001/`
