# 后端架构说明（交接版）

> 项目路径：`C:\Users\brace\Documents\work\gov\gov-project-backend`

## 1. 技术与定位

后端是 Spring Boot 2.7 单体服务，统一挂在 `/api` 下，承担以下职责：

- 登录鉴权与会话管理（Sa-Token）
- 用户/部门/角色/菜单权限（RBAC）
- 项目管理与状态流转
- Flowable 审批工作流
- 地图汇总与点位接口
- 审计日志与前端监控日志
- MinIO 文件上传

核心依赖见 `pom.xml`：Spring Boot、MyBatis-Plus、Sa-Token、Flowable、MinIO、MariaDB/openGauss。

## 2. 代码分层

主包：`com.gov`

- `common`：统一返回 `R`、全局异常、过滤器、定时任务
- `config`：框架配置（Sa-Token/MyBatis/MinIO/Swagger/加密/启动补丁）
- `crypto`：密码摘要、SM4 类型处理器
- `module/system`：登录、用户、部门、角色、审计、前端监控
- `module/project`：项目领域接口、实体、DTO/VO
- `module/flow`：审批流服务与接口
- `module/file`：文件上传接口

典型调用链：`Controller -> Service -> Mapper -> DB`

## 3. 请求链路（从进站到落库）

1. `TraceIdFilter`：生成/透传 `X-Trace-Id`
2. `SaTokenConfig`：统一登录拦截（登录与文档接口放行）
3. 业务 Controller
4. `AuditAccessFilter`：记录审计 + 慢请求告警 + `sys_audit_log` 落库
5. `GlobalExceptionHandler`：统一输出 `R(code,msg,data)`

## 4. 关键领域

## 4.1 认证与权限

- 登录：`/system/login`
- 当前用户：`/system/me`
- 登出：`/system/logout`
- 权限快照收口在 `SysUserServiceImpl#getAccessContext`
  - 标准角色：`admin / dept_leader / user`
  - 菜单键：如 `project:manage`, `system:user`, `system:audit`

## 4.2 项目管理

接口前缀：`/project`

- `add / page / get/{id} / update / {id}(delete) / submit`
- 地图：`map/list`、`map/summary`

状态机：

- `0` 待提交
- `1` 审批中
- `2` 已通过
- `3` 已驳回

规则：

- 编辑：仅 `0/3`
- 删除：`0/3`，管理员可删 `2`
- 地图默认只看 `2`

## 4.3 审批流

- 查询待办：`/flow/todo`
- 查询已办：`/flow/done`
- 审批动作：`/flow/approve`

流程定义文件：`src/main/resources/processes/project_approval.bpmn20.xml`

关键流程变量：

- `currentAssignee` 当前处理人
- `approved` 是否通过
- `hasNext` 是否存在下一级审批

流程推进与业务状态同步在 `FlowService` 完成。

## 4.4 系统管理域

- 用户：`/system/user/*`
- 部门：`/system/dept/*`
- 角色：`/system/role/*`
- 审计：`/system/audit/page`
- 前端监控：`/system/frontend-monitor/report|page`

## 5. 数据与安全

- 数据库：默认 MariaDB（可切 openGauss）
- 主键：MyBatis-Plus `ASSIGN_ID`
- 逻辑删除：`deleted`
- 自动时间填充：`createTime/updateTime`

敏感信息：

- 密码：`PasswordCrypto`，SM3 摘要（含 `username + pepper`）
- 手机号：`SmTypeHandler`，SM4 透明加解密

## 6. 观测与运维

- 日志：`logback-spring.xml`
- 审计日志：`com.gov.audit`
- 性能日志：`com.gov.perf`
- 数据库日志保留清理：`LogRetentionCleanupTask`
- 启动补丁：`SchemaPatchRunner`（字段/索引自动补齐）

## 7. 新需求改造入口

- 新业务接口：`module/*/controller` + `service`
- 新权限点：`SysUserServiceImpl#getAccessContext` + `sys_role.menu_perms`
- 新审批规则：`FlowService` + `project_approval.bpmn20.xml`
- 新审计字段：`AuditAccessFilter` + `sys_audit_log`
- 新加密字段：实体 `@TableField(typeHandler=SmTypeHandler.class)`

## 8. 建议阅读顺序

1. `application.yml`
2. `SaTokenConfig` + `SysUserServiceImpl`
3. `ProjectController` + `FlowService`
4. `SysUserController / SysDeptController / SysRoleController`
5. `AuditAccessFilter` + `SysAuditController / SysFrontendMonitorController`
