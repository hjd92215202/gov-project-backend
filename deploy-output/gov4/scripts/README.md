# ARM 麒麟 Docker 部署说明

## 镜像约定
- MariaDB：`cr.kylinos.cn/basekylin/mariadb:10.3.39-ky10sp3_2403`
- Nginx：`cr.kylinos.cn/basekylin/nginx:1.21.5-ky10sp3_2403`
- Java：`cr.kylinos.cn/basekylin/java-openjdk-8-aarch64:1.8.0-v10sp1`
- MinIO：`minio/minio:RELEASE.2023-08-09T23-30-22Z`

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
- 公网仅暴露 `81` 端口，前端页面、后端接口和附件访问全部统一走 Nginx。
- `GOV_MINIO_ENDPOINT` 必须填写容器网络内地址，例如 `http://gov4-minio:9000`。
- `GOV_MINIO_PUBLIC_ENDPOINT` 填写浏览器访问地址，例如 `http://220.154.137.90:81/minio`。
- 前端运行时配置放在 `/opt/gov4/frontend/frontend.env`，修改后重新执行 `sh /opt/gov4/scripts/run-frontend.sh` 即可生效，无需重新构建前端。
- MinIO 不直接暴露公网端口，附件下载和图片查看通过 `http://220.154.137.90:81/minio/...` 反向代理访问。
- MinIO 控制台只绑定到服务器本机 `127.0.0.1:9001`，如需进入控制台，请在服务器本机访问，或通过 SSH 隧道转发后访问。
- 后端启动时会自动检查并创建 `gov-files` 桶。
- `run-mariadb.sh`、`wait-mariadb-and-init.sh` 和 `run-minio.sh` 会优先读取 `/opt/gov4/backend/backend.env`，避免数据库和对象存储账号与后端配置不一致。
- 首次部署时会自动完成数据库等待、建库、建用户、授权和 `RBAC.sql` 导入，后续重复部署会自动跳过已有结构，不再需要手工进库授权。
- `run-frontend.sh` 会根据 `/opt/gov4/frontend/frontend.env` 生成 `/opt/gov4/frontend/runtime/env.js`，供浏览器启动时读取。
- 如果你之前已经导入过乱码数据，修复字符集后仍需清空 `/opt/gov4/mariadb/data/*` 再重新部署，才能把演示数据重新按正确中文导入。

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
