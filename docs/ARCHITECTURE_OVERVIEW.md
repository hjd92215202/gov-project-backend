# 后端架构总览

## 1. 整体分层图

```mermaid
flowchart TD
    A[前端 / 外部调用] --> B[Controller 接口层]
    B --> C[DTO 请求对象]
    B --> D[Service 业务层]
    D --> E[Mapper / MyBatis-Plus]
    E --> F[(MariaDB)]

    D --> G[Flowable 审批引擎]
    D --> H[Sa-Token 登录鉴权]
    D --> I[MinIO 文件存储]

    B --> J[VO 响应对象]
    B --> K[R 统一返回结构]
    B --> L[GlobalExceptionHandler 全局异常处理]

    M[SchemaPatchRunner] --> F
    N[processes/projectApproval.bpmn20.xml] --> G
```

## 2. 模块拆分图

```mermaid
flowchart LR
    A[system 模块]
    B[project 模块]
    C[flow 模块]
    D[file 模块]
    E[common/config]

    A --> A1[登录 / 当前用户]
    A --> A2[用户管理]
    A --> A3[部门管理]
    A --> A4[角色管理]

    B --> B1[项目新增/编辑/删除]
    B --> B2[项目分页 / 详情 / 地图]
    B --> B3[项目提交审批]

    C --> C1[我的待办]
    C --> C2[我的已办]
    C --> C3[审批通过/驳回]

    D --> D1[文件上传]

    E --> E1[全局异常处理]
    E --> E2[数据库补丁与索引]
    E --> E3[Sa-Token / 基础配置]

    B --> C
    A --> B
    A --> C
    D --> I[MinIO]
```

## 3. 核心目录职责

```text
src/main/java/com/gov/
├─ common/
│  ├─ result/R.java                 统一响应包装
│  └─ exception/GlobalExceptionHandler.java
├─ config/
│  └─ SchemaPatchRunner.java        启动时索引/补丁执行
├─ module/
│  ├─ system/
│  │  ├─ controller/                登录、用户、部门、角色接口
│  │  ├─ dto/                       用户/部门/角色请求 DTO
│  │  ├─ entity/                    系统实体
│  │  ├─ mapper/                    MyBatis-Plus Mapper
│  │  ├─ service/                   系统业务服务
│  │  └─ vo/                        用户/角色/部门响应 VO + 访问上下文
│  ├─ project/
│  │  ├─ controller/                项目接口
│  │  ├─ dto/                       项目请求 DTO
│  │  ├─ entity/                    项目实体
│  │  ├─ mapper/                    项目 Mapper
│  │  ├─ service/                   项目服务
│  │  └─ vo/                        项目分页/详情/地图/审批任务 VO
│  ├─ flow/
│  │  ├─ controller/                待办、已办、审批接口
│  │  └─ service/                   Flowable 审批流推进
│  └─ file/
│     └─ controller/                MinIO 上传接口
└─ GovApplication.java

src/main/resources/
├─ application.yml                  数据库、MinIO、Flowable、Sa-Token 配置
└─ processes/
   └─ projectApproval.bpmn20.xml    项目审批流程定义
```

## 4. 认证与权限链路

```mermaid
sequenceDiagram
    participant FE as 前端
    participant LC as LoginController
    participant US as SysUserServiceImpl
    participant ST as Sa-Token
    participant DB as MariaDB

    FE->>LC: POST /system/login
    LC->>US: login(username, password)
    US->>DB: 查询 sys_user
    US->>US: SM3(password + username) 校验
    US->>ST: StpUtil.login(userId)
    LC->>US: getAccessContext(userId)
    US->>DB: 查询用户角色 / 角色菜单 / 部门负责人关系
    LC-->>FE: token + userInfo + roleCodes + menuKeys
```

### 权限判定要点

- 登录态由 Sa-Token 托管，前端通过 `Authorization` 头传 token。
- `UserAccessContext` 是当前后端权限收口核心，统一包含：
  - `userId`
  - `deptId`
  - `roleIds`
  - `roleCodes`
  - `menuKeys`
  - `isAdmin`
  - `isDeptLeader`
- 管理员看全量数据。
- 部门负责人看本部门范围数据。
- 普通用户只看自己创建的数据。

## 5. 项目与审批主链路

### 项目 CRUD 与分页

```mermaid
sequenceDiagram
    participant FE as 前端项目页
    participant PC as ProjectController
    participant DTO as ProjectCreateDTO / ProjectUpdateDTO
    participant PS as BizProjectService
    participant DB as biz_project

    FE->>PC: 新增/更新/删除/分页/详情
    PC->>DTO: 绑定请求参数
    PC->>PC: 权限范围校验 + 状态机校验
    PC->>PS: 调用项目服务
    PS->>DB: CRUD / 分页查询
    PC-->>FE: ProjectPageVO / ProjectDetailVO / 中文结果消息
```

### 提交审批与逐级流转

```mermaid
sequenceDiagram
    participant FE as 前端提交审批
    participant PC as ProjectController
    participant FS as FlowService
    participant FL as Flowable
    participant DB as biz_project
    participant DEPT as sys_dept / sys_user

    FE->>PC: POST /project/submit
    PC->>DB: 校验项目存在、状态可提交、权限可操作
    PC->>DEPT: 获取当前用户部门与部门负责人
    PC->>DB: 项目状态改为 1 审批中
    PC->>FS: startProcess(projectId, currentAssignee)
    FS->>FL: 启动 projectApproval 流程
    FL-->>FS: 创建审批实例
    PC-->>FE: 提交审批成功

    FE->>PC: POST /flow/approve
    PC->>FS: approve(taskId, approved)
    FS->>FL: 完成当前任务
    FS->>DEPT: 查上级部门负责人
    alt 仍有上级负责人
        FS->>DB: 项目状态保持审批中
        FS->>FL: 指派下一节点审批人
    else 审批链结束
        FS->>DB: 项目状态改为已通过
    end
```

## 6. 数据与接口契约

### 输入契约

- Controller 请求体已按 DTO 分离，不再直接拿实体接收高频写接口。
- 代表性 DTO：
  - `ProjectCreateDTO / ProjectUpdateDTO / ProjectSubmitDTO`
  - `UserCreateDTO / UserUpdateDTO / UserStatusUpdateDTO / UserRoleAssignDTO`
  - `RoleCreateDTO / RoleUpdateDTO / RoleMenuUpdateDTO`
  - `DeptCreateDTO / DeptUpdateDTO`

### 输出契约

- 列表和详情接口已按 VO 收口，避免实体字段直接泄露。
- 代表性 VO：
  - `ProjectPageVO / ProjectDetailVO / ProjectMapVO`
  - `UserPageVO / UserSimpleVO`
  - `RolePageVO / RoleOptionVO`
  - `SysDeptTreeVO`
  - `FlowTaskVO`

### 统一返回

- 所有接口统一返回 `R(code, msg, data)`。
- `msg` 现在已经统一收口为中文，前端可直接展示。

## 7. 外部依赖图

```mermaid
flowchart LR
    A[Spring Boot 应用]
    B[(MariaDB)]
    C[Flowable Engine]
    D[MinIO]
    E[Sa-Token]

    A --> B
    A --> C
    A --> D
    A --> E
```

## 8. 当前后端的关键约束

- 项目状态机固定：`0 待提交`、`1 审批中`、`2 已通过`、`3 已驳回`。
- 仅 `0/3` 状态允许编辑、删除、再次提交。
- 地图接口默认只返回已通过项目。
- 用户权限以角色码、菜单权限、部门负责人兜底三层组合判断。
- 启动阶段会通过 `SchemaPatchRunner` 做索引与补丁收敛。
