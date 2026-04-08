# 后端安全整改报告

## 规范来源
- `JAVA语言安全编程规范.txt`
- `JAVA编程规范`
- 本轮漏洞修复方案：Cookie 会话、CSRF、附件授权、密码迁移、可信代理 IP、文档暴露治理

## 半成品接管说明
本轮是在已有后端半成品状态上续做，保留并继续演进以下已落地改动：
- `BizException`
- `SecurityConfigurationValidator`
- DTO Bean Validation 与部分控制器 `@Validated/@Valid`
- `application.yml` 敏感默认值清理
- 部分 `RuntimeException`、`catch ignored`、`Calendar` 治理

## 命中问题
- 认证仍可被前端持久化 token 主链路绕开，缺少 Cookie + CSRF 收口
- `/common/upload` 仍是通用上传入口
- 项目附件存在回退到 MinIO 公开地址的风险
- 临时附件缺少明确上传人归属控制
- 登录密码仍需兼容历史 SM3 并平滑升级到更强哈希
- 限流、审计、前端监控的客户端 IP 解析缺少统一可信代理边界
- 部署模板、备份脚本和文档仍残留默认值或旧行为描述

## 改造动作
- 登录主链切换到 `HttpOnly Cookie`，增加 `SameSite=Lax` 与可配置 `Secure`
- 新增双提交 Cookie CSRF 校验过滤器，并在登录/`/system/me` 下发 `XSRF-TOKEN`
- 删除 `/common/upload`，仅保留项目附件上传入口
- 项目附件访问改为后端预览/下载接口，`ProjectFileVO.previewUrl/downloadUrl/accessUrl` 均指向受控地址
- `SysFile` 新增 `creatorUserId`，未绑定业务的临时附件只允许上传者本人下载、清理和绑定
- 密码编码升级到 BCrypt，兼容历史 SM3 与旧无 pepper SM3，登录成功后自动升级
- 登录失败统一收口为同类消息，避免账号枚举
- 新增 `ClientIpResolver`，仅在远端地址属于可信代理时解析 `X-Forwarded-For` / `X-Real-IP`
- Swagger/Knife4j 改为配置驱动，部署默认关闭，放行规则只在启用文档时生效
- 更新 `backend.env.example`、`README.md`、`RBAC.sql`、`db_backup.py` 与门禁脚本

## 验证结果
- 安全门禁：PASS
  - `powershell -ExecutionPolicy Bypass -File .\scripts\security-remediation-check.ps1`
- 后端测试：PASS
  - `powershell -ExecutionPolicy Bypass -File .\scripts\mvn-jdk8.ps1 -q test`
- 后端打包：PASS
  - `powershell -ExecutionPolicy Bypass -File .\scripts\package-kylin-arm.ps1`
- 交付包刷新：PASS
  - `deploy-output/gov4/backend/app.jar`
  - `deploy-output/gov4/backend/backend.env`
  - `deploy-output/gov4/scripts/README.md`
  - `deploy-output/gov4/mariadb/init/RBAC.sql`

## 未完成项
- 待生成 backend Git commit 后回填 commit id

## 剩余风险
- 本轮按纯 Web 同源场景收口，不保留外部长期 `Authorization` token 客户端兼容链路
- 生产部署必须显式维护 `backend.env`，否则应用会因缺失敏感配置启动失败
- 如运维侧仍保留 `/minio/` 公网代理，仅可作排障用途，业务代码不应回退使用该地址

## 交付包位置
- 源码: `gov-project-backend`
- 部署模板: `deploy/kylin-arm/`
- 交付目录: `deploy-output/gov4/`

## 对应 Commit
- backend commit: pending
