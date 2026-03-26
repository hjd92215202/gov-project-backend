-- 1. 部门表
CREATE TABLE sys_dept (
                          id BIGINT NOT NULL,
                          parent_id BIGINT DEFAULT 0,
                          dept_name VARCHAR(50) NOT NULL,
                          leader_id BIGINT, -- 部门负责人ID，后续走工作流审批需要用到
                          deleted SMALLINT DEFAULT 0,
                          create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          PRIMARY KEY (id)
);
COMMENT ON TABLE sys_dept IS '部门表';
COMMENT ON COLUMN sys_dept.parent_id IS '父部门ID(顶级为0)';

-- 2. 角色表
CREATE TABLE sys_role (
                          id BIGINT NOT NULL,
                          role_name VARCHAR(50) NOT NULL,
                          role_code VARCHAR(50) NOT NULL, -- 角色编码，如 admin, user, manager
                          deleted SMALLINT DEFAULT 0,
                          create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          PRIMARY KEY (id)
);
COMMENT ON TABLE sys_role IS '角色表';

-- 3. 用户表
CREATE TABLE sys_user (
                          id BIGINT NOT NULL,
                          dept_id BIGINT,
                          username VARCHAR(50) NOT NULL,
                          password VARCHAR(255) NOT NULL, -- 后面我们会存入 SM3 盐值加密后的密文
                          real_name VARCHAR(50),
                          phone VARCHAR(20),
                          status SMALLINT DEFAULT 1, -- 1:正常, 0:停用
                          deleted SMALLINT DEFAULT 0,
                          create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          PRIMARY KEY (id)
);
COMMENT ON TABLE sys_user IS '用户表';

-- 4. 用户-角色关联表 (多对多)
CREATE TABLE sys_user_role (
                               id BIGINT NOT NULL,
                               user_id BIGINT NOT NULL,
                               role_id BIGINT NOT NULL,
                               PRIMARY KEY (id)
);
COMMENT ON TABLE sys_user_role IS '用户角色关联表';

-- 初始化一个超级管理员账号 (密码暂时明文，假设后续再改密，或直接写入一个测试账号)
-- 注意：这里的ID只是示例，真实情况由雪花算法生成
INSERT INTO sys_role (id, role_name, role_code) VALUES (1, '系统管理员', 'admin');
INSERT INTO sys_dept (id, parent_id, dept_name) VALUES (1, 0, '总经办');
INSERT INTO sys_user (id, dept_id, username, password, real_name, status, deleted)
VALUES (1, 1, 'admin', '66d7edc85a32d756ffef0046a56cf78060276b6beb3f02de7916c01ad54ea6b0', '超级管理员', 1, 0);



-- 1. 工程项目表
CREATE TABLE biz_project (
                             id BIGINT NOT NULL,
                             project_name VARCHAR(200) NOT NULL, -- 工程名称
                             project_code VARCHAR(100),          -- 工程编号
                             address VARCHAR(500),               -- 工程地址
                             longitude DECIMAL(10, 7),           -- 经度
                             latitude DECIMAL(10, 7),            -- 纬度
                             leader_name VARCHAR(50),            -- 负责人
                             leader_phone VARCHAR(255),          -- 负责人电话 (我们要加密这个字段)
                             description TEXT,                   -- 工程描述
                             status INT DEFAULT 0,               -- 状态：0待提交, 1审批中, 2已通过, 3被驳回
                             deleted SMALLINT DEFAULT 0,
                             create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             PRIMARY KEY (id)
);

-- 2. 公共文件表 (用于存储图片和ZIP包的路径)
CREATE TABLE sys_file (
                          id BIGINT NOT NULL,
                          biz_id BIGINT,                      -- 关联的业务ID (如工程ID)
                          file_name VARCHAR(255),             -- 原始文件名
                          file_path VARCHAR(500),             -- MinIO 中的存储路径
                          file_type VARCHAR(50),              -- 文件类型 (image/zip)
                          file_size BIGINT,                   -- 文件大小
                          create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          PRIMARY KEY (id)
);