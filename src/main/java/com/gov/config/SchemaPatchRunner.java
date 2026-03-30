package com.gov.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 职责：在应用启动阶段补齐历史库缺失的字段与索引。
 * 为什么存在：不同环境的建库时间不一致，容易出现“字段已升级但索引没跟上”的情况，
 * 这里统一做启动期兜底，保证性能优化项可以尽快生效。
 */
@Component
public class SchemaPatchRunner implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 启动后执行结构补丁。
     *
     * @param args 启动参数
     */
    @Override
    public void run(String... args) {
        safeExec("ALTER TABLE sys_role ADD COLUMN IF NOT EXISTS menu_perms VARCHAR(1000) DEFAULT NULL COMMENT '菜单权限键集合（逗号分隔）'");
        safeExec("ALTER TABLE biz_project ADD COLUMN IF NOT EXISTS creator_id BIGINT DEFAULT NULL COMMENT '创建人用户标识'");
        safeExec("ALTER TABLE biz_project ADD COLUMN IF NOT EXISTS creator_dept_id BIGINT DEFAULT NULL COMMENT '创建人部门标识'");
        safeExec("CREATE TABLE IF NOT EXISTS sys_audit_log ("
                + "id BIGINT NOT NULL,"
                + "user_id BIGINT DEFAULT NULL,"
                + "request_method VARCHAR(16) DEFAULT NULL,"
                + "request_uri VARCHAR(255) DEFAULT NULL,"
                + "client_ip VARCHAR(64) DEFAULT NULL,"
                + "user_agent VARCHAR(512) DEFAULT NULL,"
                + "http_status INT DEFAULT NULL,"
                + "duration_ms BIGINT DEFAULT NULL,"
                + "trace_id VARCHAR(64) DEFAULT NULL,"
                + "request_time DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (id)"
                + ")");
        safeExec("CREATE TABLE IF NOT EXISTS sys_frontend_log ("
                + "id BIGINT NOT NULL,"
                + "user_id BIGINT DEFAULT NULL,"
                + "log_level VARCHAR(16) DEFAULT NULL,"
                + "log_type VARCHAR(32) DEFAULT NULL,"
                + "event_name VARCHAR(64) DEFAULT NULL,"
                + "message VARCHAR(255) DEFAULT NULL,"
                + "page_path VARCHAR(255) DEFAULT NULL,"
                + "trace_id VARCHAR(64) DEFAULT NULL,"
                + "detail_json TEXT DEFAULT NULL,"
                + "client_ip VARCHAR(64) DEFAULT NULL,"
                + "user_agent VARCHAR(512) DEFAULT NULL,"
                + "created_time DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (id)"
                + ")");

        safeExec("ALTER TABLE sys_user MODIFY phone VARCHAR(255) DEFAULT NULL COMMENT '手机号（数据库中为密文）'");
        safeExec("CREATE INDEX idx_sys_user_username ON sys_user(username)");
        safeExec("CREATE INDEX idx_sys_user_dept_status ON sys_user(dept_id, status)");
        safeExec("CREATE INDEX idx_sys_user_role_user_id ON sys_user_role(user_id)");
        safeExec("CREATE INDEX idx_sys_user_role_role_id ON sys_user_role(role_id)");
        safeExec("CREATE INDEX idx_sys_role_role_code ON sys_role(role_code)");
        safeExec("CREATE INDEX idx_sys_dept_parent_id ON sys_dept(parent_id)");
        safeExec("CREATE INDEX idx_sys_dept_leader_id ON sys_dept(leader_id)");
        safeExec("CREATE INDEX idx_biz_project_creator_status_time ON biz_project(creator_id, status, create_time)");
        safeExec("CREATE INDEX idx_biz_project_creator_dept_status_time ON biz_project(creator_dept_id, status, create_time)");
        safeExec("CREATE INDEX idx_biz_project_status_region ON biz_project(status, province, city, district)");
        safeExec("CREATE INDEX idx_biz_project_status_deleted_region ON biz_project(status, deleted, province, city, district)");
        safeExec("CREATE INDEX idx_biz_project_status_deleted_creator_region ON biz_project(status, deleted, creator_id, province, city, district)");
        safeExec("CREATE INDEX idx_biz_project_status_deleted_dept_region ON biz_project(status, deleted, creator_dept_id, province, city, district)");
        safeExec("CREATE INDEX idx_sys_audit_log_time ON sys_audit_log(request_time)");
        safeExec("CREATE INDEX idx_sys_audit_log_user_time ON sys_audit_log(user_id, request_time)");
        safeExec("CREATE INDEX idx_sys_audit_log_uri_time ON sys_audit_log(request_uri, request_time)");
        safeExec("CREATE INDEX idx_sys_audit_log_method_time ON sys_audit_log(request_method, request_time)");
        safeExec("CREATE INDEX idx_sys_frontend_log_time ON sys_frontend_log(created_time)");
        safeExec("CREATE INDEX idx_sys_frontend_log_user_time ON sys_frontend_log(user_id, created_time)");
        safeExec("CREATE INDEX idx_sys_frontend_log_level_time ON sys_frontend_log(log_level, created_time)");
        safeExec("CREATE INDEX idx_sys_frontend_log_type_time ON sys_frontend_log(log_type, created_time)");
        safeExec("CREATE INDEX idx_sys_frontend_log_trace_id ON sys_frontend_log(trace_id)");

        safeExec("UPDATE sys_role SET menu_perms = "
                + "'dashboard:view,project:manage,project:engineering,system:user,system:dept,system:role,system:audit,system:frontend-monitor' "
                + "WHERE role_code IN ('admin','administrator','super_admin','superadmin','role_admin') "
                + "AND (menu_perms IS NULL OR menu_perms = '')");
        safeExec("UPDATE sys_role SET menu_perms = CONCAT(menu_perms, ',system:audit') "
                + "WHERE role_code IN ('admin','administrator','super_admin','superadmin','role_admin') "
                + "AND menu_perms NOT LIKE '%system:audit%'");
        safeExec("UPDATE sys_role SET menu_perms = CONCAT(menu_perms, ',system:frontend-monitor') "
                + "WHERE role_code IN ('admin','administrator','super_admin','superadmin','role_admin') "
                + "AND menu_perms NOT LIKE '%system:frontend-monitor%'");
    }

    /**
     * 安全执行单条 SQL。
     * 这批补丁以“尽量补齐”为目标，重复创建索引或字段时不应阻塞服务启动。
     *
     * @param sql 待执行 SQL
     */
    private void safeExec(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ignored) {
            // 启动补丁失败时静默忽略，避免阻塞应用启动。
        }
    }
}
