# 安全与日志运维说明

## 国密配置
- 配置项：`gov.security.sm4.*`
- 关键变量：
  - `GOV_SM4_KEY`：SM4 密钥（16位）
  - `GOV_SM4_ENABLED`：是否启用 SM4
  - `GOV_SM4_ALLOW_PLAINTEXT_FALLBACK`：是否兼容历史明文
- 作用范围：
  - `biz_project.leader_phone`
  - `sys_user.phone`

## 密码摘要配置
- 配置项：`gov.security.password.pepper`
- 关键变量：`GOV_PASSWORD_PEPPER`
- 说明：登录校验兼容历史无 pepper 摘要，便于平滑升级。

## 日志配置
- 配置文件：`src/main/resources/logback-spring.xml`
- 日志目录：`logging.file.path`（默认 `./logs`）
- 日志文件：
  - `app.log`：应用综合日志
  - `error.log`：错误日志
  - `audit.log`：接口审计日志
- 滚动策略（可配置）：
  - `gov.logging.max-history-days`
  - `gov.logging.max-file-size`
  - `gov.logging.total-size-cap`

## 审计链路
- `TraceIdFilter`：为每个请求写入 `X-Trace-Id`，并注入 MDC。
- `AuditAccessFilter`：记录 `method/uri/userId/status/durationMs`。
- 日志编码统一为 `UTF-8`，支持审计检索与跨平台查看。
