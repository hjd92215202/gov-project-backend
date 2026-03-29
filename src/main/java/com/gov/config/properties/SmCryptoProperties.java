package com.gov.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 职责：承载国密 SM4 加密相关配置。
 * 为什么存在：把密钥、开关和历史兼容策略从代码中抽离，便于运维按环境配置。
 * 关键输入输出：输入为 application.yml 中的 gov.security.sm4 配置，输出为可注入的配置对象。
 * 关联链路：MyBatis 字段加解密（SmTypeHandler）。
 */
@ConfigurationProperties(prefix = "gov.security.sm4")
public class SmCryptoProperties {

    /**
     * SM4 密钥，要求 16 位。
     */
    private String key = "1234567812345678";

    /**
     * 是否启用 SM4 加解密。
     */
    private boolean enabled = true;

    /**
     * 解密失败时是否回退为明文。
     * 打开后可兼容历史明文数据，关闭后能更严格暴露异常数据。
     */
    private boolean allowPlaintextFallback = true;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAllowPlaintextFallback() {
        return allowPlaintextFallback;
    }

    public void setAllowPlaintextFallback(boolean allowPlaintextFallback) {
        this.allowPlaintextFallback = allowPlaintextFallback;
    }
}
