# 安全规范整改实施计划

## 背景
本轮整改是在后端已经存在半成品改动的基础上续做，不回滚已有修改，重点把未闭环的安全与规范改造收口，并同步刷新交付包。

## 已接管的半成品改动
- 已引入 `BizException`
- 已新增 `SecurityConfigurationValidator`
- 已新增 DTO 校验常量、`LoginDTO`、`FlowApproveDTO`
- 已在登录、审批、项目、用户、部门、角色、前端监控等入口补充 `@Validated` / `@Valid`
- 已在 `application.yml` 去除数据库密码、MinIO 凭据、SM4 key 的默认演示值
- 已将多处裸 `RuntimeException` 替换为 `BizException`
- 已将一批静默吞异常、`Calendar` 调用和坐标校验问题纳入收口

## 本轮收口项
- 保持 `SysUser.password` 字段数据库映射不变，仅通过 `@JsonIgnore` 禁止 JSON 序列化
- 继续清理静默异常，统一为“记录上下文日志 + 合理降级”
- 新增最小门禁脚本，扫描敏感默认值、`throw new RuntimeException`、`catch (Exception ignored)`
- 清理部署链路默认口令，包括 `backend.env.example`、`run-mariadb.sh`、`run-minio.sh`
- 更新部署说明，明确环境变量必填策略和缺失时的失败行为
- 刷新交付包，并将源码、文档、交付产物统一纳入提交

## 验证计划
- 运行 `powershell -ExecutionPolicy Bypass -File scripts/security-remediation-check.ps1`
- 运行 `powershell -ExecutionPolicy Bypass -File scripts/mvn-jdk8.ps1 test`
- 运行 `powershell -ExecutionPolicy Bypass -File scripts/mvn-jdk8.ps1 -DskipTests clean package`
- 刷新 `deploy-output/gov4`
- 更新整改报告，记录验证结论与剩余风险
