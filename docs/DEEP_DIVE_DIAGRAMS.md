# 底层结构图

## 1. 数据库 ER 图

```mermaid
erDiagram
    SYS_DEPT ||--o{ SYS_DEPT : "parent_id 递归父子"
    SYS_USER }o--|| SYS_DEPT : "dept_id -> id"
    SYS_DEPT ||--o| SYS_USER : "leader_id -> id"
    SYS_USER ||--o{ SYS_USER_ROLE : "user_id -> id"
    SYS_ROLE ||--o{ SYS_USER_ROLE : "role_id -> id"
    SYS_USER ||--o{ BIZ_PROJECT : "creator_id -> id"
    SYS_DEPT ||--o{ BIZ_PROJECT : "creator_dept_id -> id"
    BIZ_PROJECT ||--o{ SYS_FILE : "biz_id -> id"

    SYS_DEPT {
        bigint id PK
        bigint parent_id
        varchar dept_name
        bigint leader_id
        smallint deleted
        datetime create_time
        datetime update_time
    }

    SYS_ROLE {
        bigint id PK
        varchar role_name
        varchar role_code
        varchar menu_perms
        smallint deleted
        datetime create_time
        datetime update_time
    }

    SYS_USER {
        bigint id PK
        bigint dept_id FK
        varchar username
        varchar password
        varchar real_name
        varchar phone
        smallint status
        smallint deleted
        datetime create_time
        datetime update_time
    }

    SYS_USER_ROLE {
        bigint id PK
        bigint user_id FK
        bigint role_id FK
    }

    BIZ_PROJECT {
        bigint id PK
        varchar project_name
        varchar project_code
        varchar address
        varchar province
        varchar city
        varchar district
        decimal longitude
        decimal latitude
        varchar leader_name
        varchar leader_phone
        text description
        int status
        bigint creator_id FK
        bigint creator_dept_id FK
        smallint deleted
        datetime create_time
        datetime update_time
    }

    SYS_FILE {
        bigint id PK
        bigint biz_id FK
        varchar file_name
        varchar file_path
        varchar file_type
        bigint file_size
        datetime create_time
    }
```

### ER 图阅读重点

- `sys_dept` 是树结构，`parent_id` 形成部门层级。
- `sys_user` 同时关联部门，部门又可通过 `leader_id` 指向负责人。
- `sys_user_role` 是用户和角色的多对多中间表。
- `biz_project` 把“创建人”和“创建部门”固化下来，后续分页、权限、地图和审批都依赖这两个字段。
- `sys_file` 目前是业务文件挂接点，`biz_id` 可关联项目等业务主键。

## 2. 权限关系图

```mermaid
flowchart TD
    A[登录用户]
    A --> B[SysUserServiceImpl.getAccessContext]

    B --> C[读取 sys_user]
    B --> D[读取 sys_user_role]
    D --> E[读取 sys_role]
    C --> F[拿到 dept_id]
    F --> G[检查是否是部门负责人]

    E --> H[归一化 role_code]
    H --> I[roleCodes]
    E --> J[汇总 menu_perms]
    J --> K[menuKeys]

    I --> L{是否 admin}
    I --> M{是否 dept_leader}
    G --> M

    L --> N[管理员]
    M --> O[部门负责人]
    A --> P[普通用户]

    N --> Q[全量数据范围]
    O --> R[本部门数据范围]
    P --> S[本人数据范围]

    N --> T[dashboard:view]
    N --> U[project:manage]
    N --> V[project:engineering]
    N --> W[system:user]
    N --> X[system:dept]
    N --> Y[system:role]

    O --> U
    O --> V
    O --> W
    O --> X

    P --> U
```

### 权限关系图阅读重点

- 后端权限不是只看角色名，而是 `roleCodes + menuKeys + 部门负责人兜底识别` 三层组合。
- 如果角色没配置菜单权限，会按 `admin / dept_leader / user` 走默认菜单集。
- 列表数据范围与按钮权限不是一回事：
  - 菜单权限决定“能不能进页面”
  - 数据范围决定“进来后能看到谁、改谁”

## 3. 审批流程图

```mermaid
flowchart TD
    A[项目创建 / 草稿状态 0] --> B[提交审批 /project/submit]
    B --> C[ProjectController 校验]
    C --> C1{项目状态是否为 0 或 3}
    C1 -->|否| Z1[拒绝提交]
    C1 -->|是| D[查当前用户部门]
    D --> E[查本部门负责人]
    E --> E1{负责人是否存在}
    E1 -->|否| Z2[拒绝提交]
    E1 -->|是| F[项目状态改为 1 审批中]
    F --> G[FlowService.startProcess]
    G --> H[Flowable processApproval]
    H --> I[approveTask 当前审批人 = currentAssignee]

    I --> J[审批人处理任务 /flow/approve]
    J --> K{approved ?}

    K -->|否| L[complete + approved=false]
    L --> M[项目状态改为 3 已驳回]
    M --> N[流程结束 rejectEnd]

    K -->|是| O[读取当前审批人所属部门]
    O --> P[查上级部门]
    P --> Q{上级部门是否存在 leader}

    Q -->|是| R[设置 hasNext=true]
    R --> S[设置 currentAssignee=上级负责人]
    S --> T[项目状态保持 1]
    T --> U[回到 approveTask 继续逐级审批]

    Q -->|否| V[设置 hasNext=false]
    V --> W[项目状态改为 2 已通过]
    W --> X[流程结束 endNode]
```

### 审批流程图阅读重点

- 审批链不是固定写死几级，而是“当前部门负责人 -> 上级部门负责人 -> 再上级负责人”的动态逐级流转。
- 驳回会直接结束流程并把项目状态改为 `3`。
- 没有更上级负责人时，本次同意就是终审通过，项目状态改为 `2`。
- 地图展示只认 `status=2`，所以审批是否结束会直接影响地图是否可见。
