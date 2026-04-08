DROP DATABASE IF EXISTS gov_db;
CREATE DATABASE gov_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE gov_db;

GRANT ALL PRIVILEGES ON gov_db.* TO 'db_user'@'%';
GRANT SHOW DATABASES, REFERENCES ON *.* TO 'db_user'@'%';
FLUSH PRIVILEGES;

DROP TABLE IF EXISTS sys_audit_log;
DROP TABLE IF EXISTS sys_frontend_log;
DROP TABLE IF EXISTS sys_file;
DROP TABLE IF EXISTS biz_project;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_dept;

CREATE TABLE sys_dept (
    id BIGINT NOT NULL COMMENT '主键',
    parent_id BIGINT DEFAULT 0 COMMENT '上级部门ID，顶级为0',
    dept_name VARCHAR(50) NOT NULL COMMENT '部门名称',
    leader_id BIGINT DEFAULT NULL COMMENT '部门负责人用户ID',
    deleted SMALLINT DEFAULT 0 COMMENT '逻辑删除标记，0正常1删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

CREATE TABLE sys_role (
    id BIGINT NOT NULL COMMENT '主键',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(50) NOT NULL COMMENT '角色编码',
    menu_perms VARCHAR(1000) DEFAULT NULL COMMENT '菜单权限键集合（逗号分隔）',
    deleted SMALLINT DEFAULT 0 COMMENT '逻辑删除标记，0正常1删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE sys_user (
    id BIGINT NOT NULL COMMENT '主键',
    dept_id BIGINT DEFAULT NULL COMMENT '所属部门ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码密文',
    real_name VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    phone VARCHAR(255) DEFAULT NULL COMMENT '手机号（数据库中为密文）',
    status SMALLINT DEFAULT 1 COMMENT '状态：1启用，0停用',
    deleted SMALLINT DEFAULT 0 COMMENT '逻辑删除标记，0正常1删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE sys_user_role (
    id BIGINT NOT NULL COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

CREATE TABLE biz_project (
    id BIGINT NOT NULL COMMENT '主键',
    project_name VARCHAR(200) NOT NULL COMMENT '项目名称',
    project_code VARCHAR(100) DEFAULT NULL COMMENT '项目编号',
    address VARCHAR(500) DEFAULT NULL COMMENT '项目地址',
    province VARCHAR(50) DEFAULT NULL COMMENT '省份',
    city VARCHAR(50) DEFAULT NULL COMMENT '城市',
    district VARCHAR(50) DEFAULT NULL COMMENT '区县',
    longitude DECIMAL(10, 7) DEFAULT NULL COMMENT '经度',
    latitude DECIMAL(10, 7) DEFAULT NULL COMMENT '纬度',
    leader_name VARCHAR(50) DEFAULT NULL COMMENT '负责人姓名',
    leader_phone VARCHAR(255) DEFAULT NULL COMMENT '负责人电话（数据库中为密文）',
    description TEXT DEFAULT NULL COMMENT '项目描述',
    status INT DEFAULT 0 COMMENT '状态：0待提交，1审批中，2已通过，3已驳回',
    creator_id BIGINT DEFAULT NULL COMMENT '创建人用户ID',
    creator_dept_id BIGINT DEFAULT NULL COMMENT '创建人部门ID',
    deleted SMALLINT DEFAULT 0 COMMENT '逻辑删除标记，0正常1删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

CREATE TABLE sys_file (
    id BIGINT NOT NULL COMMENT '主键',
    biz_id BIGINT DEFAULT NULL COMMENT '业务ID',
    creator_user_id BIGINT DEFAULT NULL COMMENT '临时附件上传人用户ID',
    file_name VARCHAR(255) DEFAULT NULL COMMENT '文件原始名称',
    file_path VARCHAR(500) DEFAULT NULL COMMENT '文件存储路径',
    file_type VARCHAR(50) DEFAULT NULL COMMENT '文件类型',
    file_size BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_sys_file_biz_id (biz_id),
    KEY idx_sys_file_creator_user_id (creator_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件表';

CREATE TABLE sys_audit_log (
    id BIGINT NOT NULL COMMENT '主键',
    user_id BIGINT DEFAULT NULL COMMENT '操作用户ID',
    request_method VARCHAR(16) DEFAULT NULL COMMENT 'HTTP方法',
    request_uri VARCHAR(255) DEFAULT NULL COMMENT '请求路径',
    client_ip VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
    user_agent VARCHAR(512) DEFAULT NULL COMMENT '客户端标识',
    http_status INT DEFAULT NULL COMMENT 'HTTP状态码',
    duration_ms BIGINT DEFAULT NULL COMMENT '请求耗时毫秒',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
    request_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '请求时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统审计日志表';

CREATE TABLE sys_frontend_log (
    id BIGINT NOT NULL COMMENT '主键',
    user_id BIGINT DEFAULT NULL COMMENT '操作用户ID',
    log_level VARCHAR(16) DEFAULT NULL COMMENT '日志级别',
    log_type VARCHAR(32) DEFAULT NULL COMMENT '日志类型',
    event_name VARCHAR(64) DEFAULT NULL COMMENT '事件名称',
    message VARCHAR(255) DEFAULT NULL COMMENT '核心消息',
    page_path VARCHAR(255) DEFAULT NULL COMMENT '页面路径',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
    detail_json TEXT DEFAULT NULL COMMENT '扩展细节JSON',
    client_ip VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
    user_agent VARCHAR(512) DEFAULT NULL COMMENT '浏览器标识',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上报时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='前端运行监控日志表';

CREATE INDEX idx_sys_dept_parent_id ON sys_dept(parent_id);
CREATE INDEX idx_sys_dept_leader_id ON sys_dept(leader_id);
CREATE INDEX idx_sys_role_role_code ON sys_role(role_code);
CREATE INDEX idx_sys_user_username ON sys_user(username);
CREATE INDEX idx_sys_user_dept_status ON sys_user(dept_id, status);
CREATE INDEX idx_sys_user_role_user_id ON sys_user_role(user_id);
CREATE INDEX idx_sys_user_role_role_id ON sys_user_role(role_id);
CREATE INDEX idx_biz_project_creator_status_time ON biz_project(creator_id, status, create_time);
CREATE INDEX idx_biz_project_creator_dept_status_time ON biz_project(creator_dept_id, status, create_time);
CREATE INDEX idx_biz_project_status_region ON biz_project(status, province, city, district);
CREATE INDEX idx_biz_project_status_deleted_region ON biz_project(status, deleted, province, city, district);
CREATE INDEX idx_biz_project_status_deleted_creator_region ON biz_project(status, deleted, creator_id, province, city, district);
CREATE INDEX idx_biz_project_status_deleted_dept_region ON biz_project(status, deleted, creator_dept_id, province, city, district);
CREATE INDEX idx_sys_audit_log_time ON sys_audit_log(request_time);
CREATE INDEX idx_sys_audit_log_user_time ON sys_audit_log(user_id, request_time);
CREATE INDEX idx_sys_audit_log_uri_time ON sys_audit_log(request_uri, request_time);
CREATE INDEX idx_sys_audit_log_method_time ON sys_audit_log(request_method, request_time);
CREATE INDEX idx_sys_frontend_log_time ON sys_frontend_log(created_time);
CREATE INDEX idx_sys_frontend_log_user_time ON sys_frontend_log(user_id, created_time);
CREATE INDEX idx_sys_frontend_log_level_time ON sys_frontend_log(log_level, created_time);
CREATE INDEX idx_sys_frontend_log_type_time ON sys_frontend_log(log_type, created_time);
CREATE INDEX idx_sys_frontend_log_trace_id ON sys_frontend_log(trace_id);

INSERT INTO sys_role (id, role_name, role_code, menu_perms)
VALUES (1, '系统管理员', 'admin', 'dashboard:view,project:manage,project:engineering,system:user,system:dept,system:role,system:audit,system:frontend-monitor');

INSERT INTO sys_dept (id, parent_id, dept_name)
VALUES (1, 0, '综合部门');

INSERT INTO sys_user (id, dept_id, username, password, real_name, status, deleted)
VALUES (1, 1, 'admin', '66d7edc85a32d756ffef0046a56cf78060276b6beb3f02de7916c01ad54ea6b0', '超级管理员', 1, 0);
