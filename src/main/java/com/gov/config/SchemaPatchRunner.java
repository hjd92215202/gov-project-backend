package com.gov.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchemaPatchRunner implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        safeExec("ALTER TABLE sys_role ADD COLUMN IF NOT EXISTS menu_perms VARCHAR(1000) DEFAULT NULL COMMENT '菜单权限键集合（逗号分隔）'");
        safeExec("ALTER TABLE biz_project ADD COLUMN IF NOT EXISTS creator_id BIGINT DEFAULT NULL COMMENT '创建人用户标识'");
        safeExec("ALTER TABLE biz_project ADD COLUMN IF NOT EXISTS creator_dept_id BIGINT DEFAULT NULL COMMENT '创建人部门标识'");
    }

    private void safeExec(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ignored) {
            // 忽略非关键的补丁执行失败
        }
    }
}
