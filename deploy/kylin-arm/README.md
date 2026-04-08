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
│   └── nginx.conf
├── mariadb
│   ├── data
│   └── init
├── minio
│   └── data
└── scripts
```

## 关键说明
- 公网统一使用 `81` 端口，对外入口由 Nginx 代理前端页面、后端接口和附件访问。
- `GOV_MINIO_ENDPOINT` 需要填写容器网络内地址，例如 `http://gov4-minio:9000`。
- `GOV_MINIO_PUBLIC_ENDPOINT` 需要填写浏览器访问地址，例如 `http://220.154.137.90:81/minio`。
- 前端运行时配置放在 `/opt/gov4/frontend/frontend.env`，修改后执行 `sh /opt/gov4/scripts/run-frontend.sh` 即可生效。
- MinIO 控制台仅绑定到 `127.0.0.1:9001`，如需远程访问请使用 SSH 隧道转发。
- `run-mariadb.sh`、`wait-mariadb-and-init.sh`、`run-minio.sh` 优先读取 `/opt/gov4/backend/backend.env`，避免数据库、对象存储与后端配置不一致。
- 首次部署会自动完成建库、建用户、授权和 `RBAC.sql` 导入；重复部署会跳过已存在结构。
- `run-frontend.sh` 会根据 `/opt/gov4/frontend/frontend.env` 生成 `/opt/gov4/frontend/runtime/env.js`。

## 环境变量要求
- `/opt/gov4/backend/backend.env` 必须在启动前手工填写完整。
- 以下配置不得保留占位值：
  - `GOV_DB_PASSWORD`
  - `MYSQL_ROOT_PASSWORD`
  - `GOV_MINIO_ACCESS_KEY`
  - `GOV_MINIO_SECRET_KEY`
  - `GOV_SM4_KEY`
- 后端启动前会检查数据库密码、MinIO 凭据和 SM4 key；缺失或仍为 `CHANGE_ME` 时会直接失败。
- `run-mariadb.sh` 和 `run-minio.sh` 不再内置演示口令，缺失配置会直接退出。

## 启动步骤
```bash
chmod +x /opt/gov4/scripts/*.sh
sh /opt/gov4/scripts/deploy-all.sh
```

## 推荐检查命令
```bash
docker ps -a
docker logs --tail 200 gov4-mariadb
docker logs --tail 200 gov4-minio
docker logs --tail 200 gov4-backend
docker logs --tail 200 gov4-frontend
```

## 访问地址
- 前端首页：`http://220.154.137.90:81/`
- 后端接口文档：`http://220.154.137.90:81/api/doc.html`
- 附件访问前缀：`http://220.154.137.90:81/minio/`
- MinIO 控制台：`http://127.0.0.1:9001/`
