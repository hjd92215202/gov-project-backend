# 后端安全整改实施计划

## 背景
本轮是在已有半成品整改基础上继续收口，不回滚已落地的 `BizException`、启动期安全配置校验、DTO Bean Validation、`@Validated/@Valid` 接入和部分异常治理。

## 规范来源
- `JAVA语言安全编程规范.txt`
- `JAVA编程规范`
- 本轮追加的真实漏洞收口要求：Cookie 会话、CSRF、防止附件公开直链、密码哈希升级、可信代理 IP、Swagger 默认关闭

## 已接管的半成品改动
- 新增 `BizException`
- 新增 `SecurityConfigurationValidator`
- 去掉 `application.yml` 中数据库密码、MinIO 凭据、SM4 key 的演示默认值
- 登录、审批及多类 DTO 已补 Bean Validation
- 控制器已开始串接 `@Validated` / `@Valid`

## 本轮整改范围
- 认证主链切换为 `HttpOnly Cookie`
- 新增双提交 Cookie CSRF 校验
- 删除 `/common/upload` 通用上传口
- 项目附件改为后端受控预览/下载
- 临时附件绑定、下载、清理增加上传人归属限制
- 密码从历史 SM3 平滑迁移到 BCrypt
- 统一客户端 IP 解析，仅信任可信代理转发头
- Swagger/Knife4j 改为配置驱动，部署默认关闭
- 更新部署模板、门禁脚本、数据库初始化脚本、备份脚本和整改报告

## 本轮约束
- 不改变现有页面 UI、交互和用户体验
- 不回滚工作区已有后端改动
- 部署模板和交付包必须反映新安全行为
- 所有源码和文档按 UTF-8 保存

## 验证计划
- `powershell -ExecutionPolicy Bypass -File .\scripts\security-remediation-check.ps1`
- `powershell -ExecutionPolicy Bypass -File .\scripts\mvn-jdk8.ps1 test`
- `powershell -ExecutionPolicy Bypass -File .\scripts\mvn-jdk8.ps1 -q -DskipTests clean package`
- 刷新 `deploy-output/gov4`
- 更新整改报告中的门禁结果、构建结果、交付包位置和 commit
