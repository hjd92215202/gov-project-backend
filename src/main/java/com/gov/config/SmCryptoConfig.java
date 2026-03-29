package com.gov.config;

import com.gov.config.properties.SmCryptoProperties;
import com.gov.crypto.SmTypeHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * 职责：在应用启动时把国密配置注入到 MyBatis 字段处理器。
 * 为什么存在：SmTypeHandler 由 MyBatis 创建，不直接走 Spring 注入，需要单独桥接配置。
 * 关键输入输出：输入为 SmCryptoProperties，输出为 SmTypeHandler 的运行时配置。
 * 关联链路：用户手机号、项目联系人手机号的入库加密与出库解密。
 */
@Configuration
@EnableConfigurationProperties(SmCryptoProperties.class)
public class SmCryptoConfig {

    private final SmCryptoProperties smCryptoProperties;

    public SmCryptoConfig(SmCryptoProperties smCryptoProperties) {
        this.smCryptoProperties = smCryptoProperties;
    }

    @PostConstruct
    public void initSmTypeHandler() {
        SmTypeHandler.configure(
                smCryptoProperties.getKey(),
                smCryptoProperties.isEnabled(),
                smCryptoProperties.isAllowPlaintextFallback()
        );
    }
}
