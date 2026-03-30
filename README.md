# 政务项目后端说明

## 1. 项目定位
该项目为政务项目管理系统后端，承担以下核心职责：
- 登录鉴权与会话管理
- 用户、部门、角色、菜单权限管理
- 项目立项、维护、删除、提交审批
- Flowable 审批流转
- 地图看板点位与区域摘要接口
- 文件上传与对象存储接入
- 审计日志、性能日志、前端监控数据接收

## 2. 技术栈
- Spring Boot 2.7
- MyBatis-Plus
- Sa-Token
- Flowable
- MariaDB / openGauss
- MinIO
- Knife4j

## 3. 默认本地依赖
### 3.1 MinIO
```powershell
docker run -d `
  -p 9000:9000 `
  -p 9001:9001 `
  --name gov-minio `
  -e "MINIO_ROOT_USER=govadmin" `
  -e "MINIO_ROOT_PASSWORD=govadminpassword" `
  -v c:\minio\data:/data `
  minio/minio:RELEASE.2023-08-09T23-30-22Z `
  server /data --console-address ":9001"
```

### 3.2 openGauss
```powershell
docker run --name opengauss --privileged=true -d `
  -e GS_PASSWORD=Egov@123 `
  -p 8888:5432 `
  -v c:\opengauss:/var/lib/opengauss `
  opengauss/opengauss:latest
```

```sql
CREATE USER db_user WITH PASSWORD 'Egov@123';
ALTER USER db_user SYSADMIN;
```

### 3.3 MariaDB
```powershell
docker run --name mdb1 `
  -v c:\mariadb\data:/var/lib/mysql `
  -p 13306:3306 `
  -e MYSQL_ROOT_PASSWORD=123 `
  -e MYSQL_USER=db_user `
  -e MYSQL_PASSWORD=Egov@123 `
  -d mariadb:10.11 `
  --character-set-server=utf8mb4 `
  --collation-server=utf8mb4_unicode_ci `
  --lower_case_table_names=1
```

## 4. 本地访问入口
- 接口文档：`http://localhost:8080/api/doc.html`
- MinIO 控制台：`http://localhost:9001/login`

## 5. 编码与维护约定
- 所有源码、配置、SQL、文档统一使用 UTF-8
- 新增接口统一返回 `R(code,msg,data)` 结构
- 返回给前端的错误文案优先使用明确中文
- 新增日志与配置注释统一中文化
