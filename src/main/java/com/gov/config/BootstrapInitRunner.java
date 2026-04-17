package com.gov.config;

import com.gov.config.properties.BootstrapProperties;
import com.gov.crypto.PasswordCrypto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * First-start bootstrap for core RBAC data and admin password alignment.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class BootstrapInitRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapInitRunner.class);

    private static final String LOCK_KEY = "gov:bootstrap:init:v1";
    private static final int LOCK_WAIT_SECONDS = 30;

    private static final String MARK_RBAC = "rbac_initialized";
    private static final String MARK_ADMIN = "admin_password_initialized";

    private final JdbcTemplate jdbcTemplate;
    private final BootstrapProperties bootstrapProperties;

    public BootstrapInitRunner(JdbcTemplate jdbcTemplate, BootstrapProperties bootstrapProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.bootstrapProperties = bootstrapProperties;
    }

    @Override
    public void run(String... args) {
        if (!bootstrapProperties.isEnabled()) {
            log.info("BootstrapInitRunner skipped because gov.bootstrap.enabled=false");
            return;
        }
        String username = normalize(bootstrapProperties.getAdmin().getUsername(), "admin");
        String initialPassword = normalize(bootstrapProperties.getAdmin().getInitialPassword(), "admin123");

        Integer locked = safeQueryForInt("SELECT GET_LOCK(?, ?)", LOCK_KEY, LOCK_WAIT_SECONDS);
        if (locked == null || locked != 1) {
            log.warn("BootstrapInitRunner failed to acquire DB lock [{}], skip current node bootstrap", LOCK_KEY);
            return;
        }

        try {
            ensureCoreTables();
            ensureBootstrapMetaTable();

            boolean markedRbac = isMarked(MARK_RBAC);
            boolean markedAdmin = isMarked(MARK_ADMIN);
            boolean hasAnyCoreData = hasAnyCoreData();

            if (!markedRbac && !hasAnyCoreData) {
                initCoreRbacData(username, initialPassword);
                mark(MARK_RBAC, "true");
                log.info("BootstrapInitRunner initialized core RBAC seed data");
                if (bootstrapProperties.isForceResetAdminOnFirstStart()) {
                    resetAdminPassword(username, initialPassword);
                    mark(MARK_ADMIN, "true");
                    log.info("BootstrapInitRunner reset admin password on first startup");
                } else {
                    mark(MARK_ADMIN, "skipped_by_config");
                    log.info("BootstrapInitRunner skipped admin password reset by configuration");
                }
                return;
            }

            if (!markedRbac) {
                mark(MARK_RBAC, "legacy_detected");
                log.warn("BootstrapInitRunner detected legacy database without bootstrap mark; mark added without data reset");
            }

            if (!markedAdmin) {
                mark(MARK_ADMIN, "legacy_detected");
                log.warn("BootstrapInitRunner detected legacy database without admin password bootstrap mark; mark added without password reset");
            }
        } finally {
            safeQueryForInt("SELECT RELEASE_LOCK(?)", LOCK_KEY);
        }
    }

    private void ensureCoreTables() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sys_dept ("
                + "id BIGINT NOT NULL,"
                + "parent_id BIGINT DEFAULT 0,"
                + "dept_name VARCHAR(50) NOT NULL,"
                + "leader_id BIGINT DEFAULT NULL,"
                + "deleted SMALLINT DEFAULT 0,"
                + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sys_role ("
                + "id BIGINT NOT NULL,"
                + "role_name VARCHAR(50) NOT NULL,"
                + "role_code VARCHAR(50) NOT NULL,"
                + "menu_perms VARCHAR(1000) DEFAULT NULL,"
                + "deleted SMALLINT DEFAULT 0,"
                + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sys_user ("
                + "id BIGINT NOT NULL,"
                + "dept_id BIGINT DEFAULT NULL,"
                + "username VARCHAR(50) NOT NULL,"
                + "password VARCHAR(255) NOT NULL,"
                + "real_name VARCHAR(50) DEFAULT NULL,"
                + "phone VARCHAR(255) DEFAULT NULL,"
                + "status SMALLINT DEFAULT 1,"
                + "deleted SMALLINT DEFAULT 0,"
                + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sys_user_role ("
                + "id BIGINT NOT NULL,"
                + "user_id BIGINT NOT NULL,"
                + "role_id BIGINT NOT NULL,"
                + "PRIMARY KEY (id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        ensureIndex("sys_role", "idx_sys_role_role_code", "role_code");
        ensureIndex("sys_user", "idx_sys_user_username", "username");
        ensureIndex("sys_user", "idx_sys_user_dept_status", "dept_id, status");
        ensureIndex("sys_user_role", "idx_sys_user_role_user_id", "user_id");
        ensureIndex("sys_user_role", "idx_sys_user_role_role_id", "role_id");
        ensureIndex("sys_dept", "idx_sys_dept_parent_id", "parent_id");
        ensureIndex("sys_dept", "idx_sys_dept_leader_id", "leader_id");
    }

    private void ensureBootstrapMetaTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sys_bootstrap_meta ("
                + "meta_key VARCHAR(64) NOT NULL,"
                + "meta_value VARCHAR(255) DEFAULT NULL,"
                + "updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (meta_key)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private boolean hasAnyCoreData() {
        return exists("SELECT 1 FROM sys_user LIMIT 1")
                || exists("SELECT 1 FROM sys_role LIMIT 1")
                || exists("SELECT 1 FROM sys_dept LIMIT 1");
    }

    private void initCoreRbacData(String username, String initialPassword) {
        Long deptId = queryLong("SELECT id FROM sys_dept WHERE deleted = 0 ORDER BY create_time ASC, id ASC LIMIT 1");
        if (deptId == null) {
            deptId = queryLong("SELECT id FROM sys_dept ORDER BY create_time ASC, id ASC LIMIT 1");
        }
        if (deptId == null) {
            deptId = 1L;
            jdbcTemplate.update("INSERT INTO sys_dept (id, parent_id, dept_name, deleted) "
                    + "SELECT ?, 0, ?, 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = ?)",
                    deptId, "General Department", deptId);
        }

        Long roleId = queryLong("SELECT id FROM sys_role WHERE role_code = ? LIMIT 1", "admin");
        if (roleId == null) {
            roleId = 1L;
            jdbcTemplate.update("INSERT INTO sys_role (id, role_name, role_code, menu_perms, deleted) "
                            + "SELECT ?, ?, 'admin', ?, 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code='admin')",
                    roleId, "System Administrator", adminMenuPerms());
        } else {
            jdbcTemplate.update("UPDATE sys_role SET menu_perms = ? WHERE id = ? AND (menu_perms IS NULL OR menu_perms = '')",
                    adminMenuPerms(), roleId);
        }

        Long userId = queryLong("SELECT id FROM sys_user WHERE username = ? LIMIT 1", username);
        if (userId == null) {
            userId = 1L;
            jdbcTemplate.update("INSERT INTO sys_user (id, dept_id, username, password, real_name, status, deleted) "
                            + "SELECT ?, ?, ?, ?, ?, 1, 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = ?)",
                    userId, deptId, username, PasswordCrypto.encode(initialPassword, username), "Super Administrator", username);
            userId = queryLong("SELECT id FROM sys_user WHERE username = ? LIMIT 1", username);
        }
        if (userId == null) {
            throw new IllegalStateException("BootstrapInitRunner failed to resolve admin user id for username=" + username);
        }
        if (roleId == null) {
            roleId = queryLong("SELECT id FROM sys_role WHERE role_code='admin' LIMIT 1");
        }
        if (roleId == null) {
            throw new IllegalStateException("BootstrapInitRunner failed to resolve admin role id");
        }

        jdbcTemplate.update("INSERT INTO sys_user_role (id, user_id, role_id) "
                        + "SELECT COALESCE((SELECT MAX(id) + 1 FROM sys_user_role), 1), ?, ? "
                        + "FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = ? AND role_id = ?)",
                userId, roleId, userId, roleId);
    }

    private void resetAdminPassword(String username, String initialPassword) {
        String encoded = PasswordCrypto.encode(initialPassword, username);
        int updated = jdbcTemplate.update("UPDATE sys_user SET password = ? WHERE username = ?",
                encoded, username);
        if (updated != 1) {
            throw new IllegalStateException("BootstrapInitRunner expected updating 1 admin row but updated " + updated);
        }
    }

    private String adminMenuPerms() {
        return "dashboard:view,project:manage,project:engineering,system:user,system:dept,system:role,system:audit,system:frontend-monitor";
    }

    private boolean isMarked(String key) {
        Integer count = safeQueryForInt("SELECT COUNT(1) FROM sys_bootstrap_meta WHERE meta_key = ?", key);
        return count != null && count > 0;
    }

    private void mark(String key, String value) {
        jdbcTemplate.update("INSERT INTO sys_bootstrap_meta(meta_key, meta_value) VALUES(?, ?) "
                        + "ON DUPLICATE KEY UPDATE meta_value = VALUES(meta_value), updated_time = CURRENT_TIMESTAMP",
                key, value);
    }

    private boolean exists(String sql, Object... args) {
        Integer count = safeQueryForInt("SELECT COUNT(1) FROM (" + sql + ") AS t", args);
        return count != null && count > 0;
    }

    private void ensureIndex(String tableName, String indexName, String columns) {
        Integer count = safeQueryForInt("SELECT COUNT(1) FROM information_schema.statistics "
                + "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?", tableName, indexName);
        if (count != null && count > 0) {
            return;
        }
        try {
            jdbcTemplate.execute("CREATE INDEX " + indexName + " ON " + tableName + "(" + columns + ")");
        } catch (DataAccessException exception) {
            log.warn("BootstrapInitRunner create index failed: {}.{} | {}", tableName, indexName, exception.getMessage());
        }
    }

    private Long queryLong(String sql, Object... args) {
        try {
            return jdbcTemplate.query(sql, rs -> rs.next() ? rs.getLong(1) : null, args);
        } catch (DataAccessException exception) {
            return null;
        }
    }

    private Integer safeQueryForInt(String sql, Object... args) {
        try {
            Number number = jdbcTemplate.queryForObject(sql, Number.class, args);
            return number == null ? null : number.intValue();
        } catch (DataAccessException exception) {
            return null;
        }
    }

    private String normalize(String value, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        return value.trim();
    }
}
