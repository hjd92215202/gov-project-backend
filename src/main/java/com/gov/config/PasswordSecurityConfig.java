package com.gov.config;

import com.gov.config.properties.PasswordSecurityProperties;
import com.gov.crypto.PasswordCrypto;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * 职责：启动时初始化密码摘要安全参数。
 * 为什么存在：将环境级 pepper 注入统一密码组件，避免业务层感知配置细节。
 * 关联链路：登录鉴权、用户新增、用户密码重置。
 */
@Configuration
@EnableConfigurationProperties(PasswordSecurityProperties.class)
public class PasswordSecurityConfig {

    private final PasswordSecurityProperties passwordSecurityProperties;

    public PasswordSecurityConfig(PasswordSecurityProperties passwordSecurityProperties) {
        this.passwordSecurityProperties = passwordSecurityProperties;
    }

    @PostConstruct
    public void initPasswordCrypto() {
        PasswordCrypto.configurePepper(passwordSecurityProperties.getPepper());
    }
}
