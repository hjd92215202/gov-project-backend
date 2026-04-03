package com.gov.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 职责：在应用启动阶段补齐历史库缺失的字段与索引。
 * 补丁失败时不再静默忽略，改为 warn 日志，便于运维发现问题。
 */
@Component
public class SchemaPatchRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaPatchRunner.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        ensureIndex("sys_user", "idx_sys_user_username", "username");
        ensureIndex("sys_user", "idx_sys_user_dept_status", "dept_id, status");
        ensureIndex("sys_user_role", "idx_sys_user_role_user_id", "user_id");
        ensureIndex("sys_user_role", "idx_sys_user_role_role_id", "role_id");
        ensureIndex("sys_role", "idx_sys_role_role_code", "role_code");
        ensureIndex("sys_dept", "idx_sys_dept_parent_id", "parent_id");
        ensureIndex("sys_dept", "idx_sys_dept_leader_id", "leader_id");
        ensureIndex("biz_project", "idx_biz_project_creator_status_time", "creator_id, status, create_time");
        ensureIndex("biz_project", "idx_biz_project_creator_dept_status_time", "creator_dept_id, status, create_time");
        ensureIndex("biz_project", "idx_biz_project_status_region", "status, province, city, district");
        ensureIndex("biz_project", "idx_biz_project_status_deleted_region", "status, deleted, province, city, district");
        ensureIndex("biz_project", "idx_biz_project_status_deleted_creator_region", "status, deleted, creator_id, province, city, district");
        ensureIndex("biz_project", "idx_biz_project_status_deleted_dept_region", "status, deleted, creator_dept_id, province, city, district");
        ensureIndex("sys_audit_log", "idx_sys_audit_log_time", "request_time");
        ensureIndex("sys_audit_log", "idx_sys_audit_log_user_time", "user_id, request_time");
        ensureIndex("sys_audit_log", "idx_sys_audit_log_uri_time", "request_uri, request_time");
        ensureIndex("sys_audit_log", "idx_sys_audit_log_method_time", "request_method, request_time");
        ensureIndex("sys_frontend_log", "idx_sys_frontend_log_time", "created_time");
        ensureIndex("sys_frontend_log", "idx_sys_frontend_log_user_time", "user_id, created_time");
        ensureIndex("sys_frontend_log", "idx_sys_frontend_log_level_time", "log_level, created_time");
        ensureIndex("sys_frontend_log", "idx_sys_frontend_log_type_time", "log_type, created_time");
        ensureIndex("sys_frontend_log", "idx_sys_frontend_log_trace_id", "trace_id");

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
        safeExec("DELETE FROM biz_project WHERE "
                + "(id = 2 AND project_code = 'GC-002' AND project_name = '西安市莲湖区老旧改造工程') "
                + "OR (id = 3 AND project_code = 'GC-003' AND project_name = '咸阳市某桥梁项目')");

        log.info("SchemaPatchRunner 执行完毕");
    }

    /**
     * 安全执行单条 SQL。
     * 补丁失败时输出 warn 日志，不再静默忽略，便于运维发现问题。
     */
    private void safeExec(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            log.warn("SchemaPatch 执行失败（可忽略重复补丁）: {} | error: {}",
                    sql.length() > 120 ? sql.substring(0, 120) + "..." : sql, e.getMessage());
        }
    }

    /**
     * 幂等补齐索引。先查 information_schema，只有索引缺失时才创建。
     */
    private void ensureIndex(String tableName, String indexName, String columns) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM information_schema.statistics "
                            + "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?",
                    Integer.class, tableName, indexName);
            if (count != null && count > 0) {
                return;
            }
            jdbcTemplate.execute("CREATE INDEX " + indexName + " ON " + tableName + "(" + columns + ")");
            log.info("SchemaPatch 创建索引成功: {}.{}", tableName, indexName);
        } catch (Exception e) {
            log.warn("SchemaPatch 创建索引失败: {}.{} | error: {}", tableName, indexName, e.getMessage());
        }
    }
}
