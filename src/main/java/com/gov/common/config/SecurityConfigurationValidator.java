package com.gov.common.config;

import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * 启动时校验关键安全配置，避免系统带着演示值或空值启动。
 */
@Component
public class SecurityConfigurationValidator {

    @Value("${spring.datasource.password:}")
    private String databasePassword;

    @Value("${minio.access-key:}")
    private String minioAccessKey;

    @Value("${minio.secret-key:}")
    private String minioSecretKey;

    @Value("${gov.security.sm4.key:}")
    private String sm4Key;

    @Value("${gov.security.sm4.enabled:true}")
    private boolean sm4Enabled;

    @PostConstruct
    public void validate() {
        List<String> missingItems = new ArrayList<String>();
        validateRequiredSecret("GOV_DB_PASSWORD", databasePassword, missingItems);
        validateRequiredSecret("GOV_MINIO_ACCESS_KEY", minioAccessKey, missingItems);
        validateRequiredSecret("GOV_MINIO_SECRET_KEY", minioSecretKey, missingItems);

        if (sm4Enabled) {
            validateRequiredSecret("GOV_SM4_KEY", sm4Key, missingItems);
            if (StrUtil.isNotBlank(sm4Key) && sm4Key.trim().length() != 16) {
                missingItems.add("GOV_SM4_KEY must be exactly 16 characters");
            }
        }

        if (!missingItems.isEmpty()) {
            throw new IllegalStateException("Missing or insecure security configuration: " + String.join("; ", missingItems));
        }
    }

    private void validateRequiredSecret(String key, String value, List<String> failures) {
        String normalized = StrUtil.trimToEmpty(value);
        if (StrUtil.isBlank(normalized)) {
            failures.add(key);
            return;
        }
        if ("CHANGE_ME".equalsIgnoreCase(normalized) || "CHANGE_ME_16_CHARS".equalsIgnoreCase(normalized)) {
            failures.add(key + " still uses placeholder");
        }
    }
}
