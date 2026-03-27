-- 创建数据库（如不存在）
DROP DATABASE IF EXISTS gov_db;
CREATE DATABASE gov_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE gov_db;

-- 1. 确保业务账号拥有库级权限
GRANT ALL PRIVILEGES ON gov_db.* TO 'db_user'@'%';

-- 2. 授予全局元数据相关权限（用于驱动探测）
GRANT SHOW DATABASES, REFERENCES ON *.* TO 'db_user'@'%';

-- 3. 刷新权限生效
FLUSH PRIVILEGES;

-- 按依赖顺序清理旧表
DROP TABLE IF EXISTS sys_file;
DROP TABLE IF EXISTS biz_project;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_dept;

-- 1. 部门表
CREATE TABLE sys_dept (
    id BIGINT NOT NULL COMMENT '主键',
    parent_id BIGINT DEFAULT 0 COMMENT '上级部门标识（顶级为0）',
    dept_name VARCHAR(50) NOT NULL COMMENT '部门名称',
    leader_id BIGINT DEFAULT NULL COMMENT '部门负责人用户标识',
    deleted SMALLINT DEFAULT 0 COMMENT '逻辑删除：0正常，1删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 2. 角色表
CREATE TABLE sys_role (
    id BIGINT NOT NULL COMMENT '主键',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(50) NOT NULL COMMENT '角色编码',
    menu_perms VARCHAR(1000) DEFAULT NULL COMMENT '菜单权限键集合（逗号分隔）',
    deleted SMALLINT DEFAULT 0 COMMENT '逻辑删除：0正常，1删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 3. 用户表
CREATE TABLE sys_user (
    id BIGINT NOT NULL COMMENT '主键',
    dept_id BIGINT DEFAULT NULL COMMENT '所属部门标识',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码密文',
    real_name VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    status SMALLINT DEFAULT 1 COMMENT '状态：1启用，0停用',
    deleted SMALLINT DEFAULT 0 COMMENT '逻辑删除：0正常，1删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 4. 用户角色关联表
CREATE TABLE sys_user_role (
    id BIGINT NOT NULL COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户标识',
    role_id BIGINT NOT NULL COMMENT '角色标识',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 5. 项目表
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
    leader_name VARCHAR(50) DEFAULT NULL COMMENT '项目负责人姓名',
    leader_phone VARCHAR(255) DEFAULT NULL COMMENT '项目负责人电话密文',
    description TEXT DEFAULT NULL COMMENT '项目描述',
    status INT DEFAULT 0 COMMENT '状态：0待提交，1审批中，2已通过，3已驳回',
    creator_id BIGINT DEFAULT NULL COMMENT '创建人用户标识',
    creator_dept_id BIGINT DEFAULT NULL COMMENT '创建人部门标识',
    deleted SMALLINT DEFAULT 0 COMMENT '逻辑删除：0正常，1删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

-- 6. 文件表
CREATE TABLE sys_file (
    id BIGINT NOT NULL COMMENT '主键',
    biz_id BIGINT DEFAULT NULL COMMENT '业务标识',
    file_name VARCHAR(255) DEFAULT NULL COMMENT '文件原始名称',
    file_path VARCHAR(500) DEFAULT NULL COMMENT '文件存储路径',
    file_type VARCHAR(50) DEFAULT NULL COMMENT '文件类型',
    file_size BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件表';

-- 初始化数据
INSERT INTO sys_role (id, role_name, role_code, menu_perms)
VALUES (1, '系统管理员', 'admin', 'dashboard:view,project:manage,project:engineering,system:user,system:dept,system:role');

INSERT INTO sys_dept (id, parent_id, dept_name)
VALUES (1, 0, '总经办');

INSERT INTO sys_user (id, dept_id, username, password, real_name, status, deleted)
VALUES (1, 1, 'admin', '66d7edc85a32d756ffef0046a56cf78060276b6beb3f02de7916c01ad54ea6b0', '超级管理员', 1, 0);

INSERT INTO biz_project (id, project_name, project_code, address, province, city, district, longitude, latitude, leader_name, leader_phone, status)
VALUES
(2, '西安市莲湖区老旧改造工程', 'GC-002', '陕西省西安市莲湖区XX路', '陕西省', '西安市', '莲湖区', 108.91000, 34.27000, '李四', '13911112222', 1),
(3, '咸阳市某桥梁项目', 'GC-003', '陕西省咸阳市秦都区YY路', '陕西省', '咸阳市', '秦都区', 108.70000, 34.33000, '王五', '13566667777', 1);
