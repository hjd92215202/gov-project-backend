package com.gov.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动阶段的数据库补丁执行器。
 * 这个类存在的原因是把“历史库结构差异”和“新增索引补齐”放到应用启动时自动收敛，
 * 避免因为不同环境初始化时间不同而出现字段或索引缺失。
 */
@Component
public class SchemaPatchRunner implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 应用启动后执行数据库补丁与索引补齐。
     *
     * @param args 启动参数
     */
    @Override
    public void run(String... args) {
        safeExec("ALTER TABLE sys_role ADD COLUMN IF NOT EXISTS menu_perms VARCHAR(1000) DEFAULT NULL COMMENT '菜单权限键集合（逗号分隔）'");
        safeExec("ALTER TABLE biz_project ADD COLUMN IF NOT EXISTS creator_id BIGINT DEFAULT NULL COMMENT '创建人用户标识'");
        safeExec("ALTER TABLE biz_project ADD COLUMN IF NOT EXISTS creator_dept_id BIGINT DEFAULT NULL COMMENT '创建人部门标识'");

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
    }

    /**
     * 安全执行单条 SQL。
     * 这里故意吞掉异常，是因为这批补丁属于“尽量补齐”的启动修复，
     * 已存在字段、已存在索引等情况不应阻断整个服务启动。
     *
     * @param sql 待执行 SQL
     */
    private void safeExec(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ignored) {
            // Ignore non-critical schema patch failures.
        }
    }
}
