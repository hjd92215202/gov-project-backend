# GOV-PROJECT-BACKEND AI CONTEXT

Purpose: This file is optimized for AI handoff. Share this file together with `gov-web/AI_CONTEXT.md` in new chats.

Repo: gov-project-backend
Stack: Spring Boot 2.7, MyBatis-Plus, Sa-Token, Flowable, MinIO, MariaDB/openGauss

## 1) What this backend does
- Authentication and session.
- User/Department/Role management.
- Project lifecycle and approval workflow integration.
- Approval task processing and project status write-back.
- File upload to MinIO.

## 2) Base configuration
- Server port: `8080`
- Context path: `/api`
- Token header: `Authorization`
- Unified response: `R(code, msg, data)`

Main config files:
- `src/main/resources/application.yml`
- `pom.xml`

## 3) Module map
### system
- Controllers:
  - `LoginController`
  - `SysUserController`
  - `SysDeptController`
  - `SysRoleController`
- Key service:
  - `SysUserServiceImpl` (role normalization, menu merge, admin/dept-leader checks)

### project
- Controller:
  - `ProjectController`
- Entity:
  - `BizProject`
- Responsibilities:
  - project CRUD/submit
  - data scope filtering
  - operation status checks
  - map list filtering

### flow
- Controller:
  - `FlowController`
- Service:
  - `FlowService`
- BPMN:
  - `src/main/resources/processes/project_approval.bpmn20.xml`

### file
- Controller:
  - `FileController`

## 4) API surface (main)
### auth
- `POST /system/login`
- `GET /system/me`
- `POST /system/logout`

### project
- `POST /project/add`
- `GET /project/page`
- `GET /project/get/{id}`
- `PUT /project/update`
- `DELETE /project/{id}`
- `POST /project/submit`
- `GET /project/map/list`

### flow
- `GET /flow/todo`
- `GET /flow/done`
- `POST /flow/approve`

### system user
- `GET /system/user/page`
- `GET /system/user/simple`
- `POST /system/user/add`
- `PUT /system/user/update`
- `PUT /system/user/status`
- `GET /system/user/{id}/roles`
- `PUT /system/user/roles`

### system dept
- `GET /system/dept/tree`
- `POST /system/dept/add`
- `PUT /system/dept/update`
- `DELETE /system/dept/{id}`

### system role
- `GET /system/role/page`
- `GET /system/role/all`
- `GET /system/role/menu-catalog`
- `POST /system/role/add`
- `PUT /system/role/update`
- `PUT /system/role/{id}/menus`
- `DELETE /system/role/{id}`

### file
- `POST /common/upload`

## 5) Permission and data scope rules
### role and identity
- `isAdmin(userId)`: role-code based after normalization.
- `isDeptLeader(userId)`: true if
  - role code includes dept leader OR
  - user is referenced by `sys_dept.leader_id`.

### menu permissions
- Menu keys are merged from role `menu_perms`.
- If no explicit menu perms exist, default menu sets are applied for admin/dept leader/user.

### project data scope
- Admin: all projects.
- Dept leader: same department projects (`creator_dept_id`).
- Normal user: own projects (`creator_id`).

### project operation statuses
- Editable/submittable/deletable statuses: `0` (draft), `3` (rejected).
- Approved (`2`) is read-only from business perspective.

### map data policy
- `/project/map/list` is filtered to approved projects only (`status = 2`).
- It also applies user data scope (admin/dept leader/normal user).

## 6) Workflow behavior (Flowable)
- Process key: `projectApproval`.
- On submit, assignee is current department leader.
- Approve path:
  - if parent department leader exists: continue to next level
  - else: set project status to `2`
- Reject path:
  - set project status to `3`

## 7) Core data tables
- `sys_user`
- `sys_role` (includes `menu_perms`)
- `sys_user_role`
- `sys_dept` (includes `leader_id`)
- `biz_project` (province/city/district, geo, leader info, status, creator fields)
- `sys_file`

SQL/bootstrap references:
- `RBAC.sql`
- `SchemaPatchRunner`

## 8) Security and crypto
- Password hashing: SM3 with username as salt.
- `BizProject.leaderPhone` uses `SmTypeHandler` for field-level crypto.
- Session/auth via Sa-Token.

## 9) Build and verification
- Compile: `mvn -q -DskipTests compile`
- Tests: `mvn test` (coverage is limited; manual integration testing is still important)

## 10) Known risks
1. Encoding history risk:
   - Some files previously had text encoding issues (mojibake) in comments/messages.
   - Keep all edits UTF-8 without BOM.
   - Follow .editorconfig on every edit; the repo encoding baseline is UTF-8 without BOM.
2. Permission complexity:
   - Menu-based permissions + role codes + dept leader fallback can conflict if changed carelessly.
3. Message consistency:
   - Error/success text has been revised many times; avoid mixed-language regressions.
4. Map policy coupling:
   - Frontend currently assumes map endpoint is approved-only.

## 11) High-change files (read first)
1. `src/main/java/com/gov/module/project/controller/ProjectController.java`
2. `src/main/java/com/gov/module/system/controller/SysUserController.java`
3. `src/main/java/com/gov/module/system/service/impl/SysUserServiceImpl.java`
4. `src/main/java/com/gov/module/system/controller/SysRoleController.java`
5. `src/main/java/com/gov/module/flow/service/FlowService.java`
6. `src/main/resources/application.yml`
7. `RBAC.sql`

## 12) AI handoff prompt (copy-ready)
- This is backend repo `gov-project-backend`.
- Read `ProjectController`, `SysUserServiceImpl`, `SysUserController`, `FlowService` first.
- Keep these invariants:
  - token header = Authorization
  - response wrapper = R
  - editable project statuses = 0 and 3
  - map endpoint defaults to approved status only
- Follow `.editorconfig`, preserve UTF-8 without BOM, and do not relax permission boundaries accidentally.
