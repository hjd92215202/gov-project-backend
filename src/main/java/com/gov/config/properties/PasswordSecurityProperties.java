package com.gov.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 职责：承载密码摘要策略配置。
 * 为什么存在：把口令加盐规则中的环境因子外置，避免硬编码。
 * 关键输入输出：输入为 application.yml 的 gov.security.password 配置。
 * 关联链路：登录校验、用户创建与重置密码。
 */
@ConfigurationProperties(prefix = "gov.security.password")
public class PasswordSecurityProperties {

    /**
     * 额外安全因子（pepper），建议按环境配置并妥善保管。
     */
    private String pepper = "";

    public String getPepper() {
        return pepper;
    }

    public void setPepper(String pepper) {
        this.pepper = pepper;
    }
}
