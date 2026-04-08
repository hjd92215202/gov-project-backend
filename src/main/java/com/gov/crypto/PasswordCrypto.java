package com.gov.crypto;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SmUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 统一密码摘要与校验策略，兼容历史 SM3 摘要并平滑升级到 BCrypt。
 */
public final class PasswordCrypto {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(12);
    private static volatile String pepper = "";

    private PasswordCrypto() {
    }

    public static synchronized void configurePepper(String configuredPepper) {
        pepper = configuredPepper == null ? "" : configuredPepper.trim();
    }

    public static String encode(String rawPassword, String username) {
        return PASSWORD_ENCODER.encode(normalize(rawPassword) + pepper);
    }

    public static boolean matches(String rawPassword, String username, String storedDigest) {
        if (StrUtil.isBlank(storedDigest)) {
            return false;
        }
        String normalizedStoredDigest = storedDigest.trim();
        if (isBcryptDigest(normalizedStoredDigest)) {
            return PASSWORD_ENCODER.matches(normalize(rawPassword) + pepper, normalizedStoredDigest);
        }
        return matchesLegacyDigest(rawPassword, username, normalizedStoredDigest);
    }

    public static boolean needsUpgrade(String storedDigest) {
        return !isBcryptDigest(StrUtil.trimToEmpty(storedDigest));
    }

    public static boolean isBcryptDigest(String storedDigest) {
        if (StrUtil.isBlank(storedDigest)) {
            return false;
        }
        String normalized = storedDigest.trim();
        return normalized.startsWith("$2a$") || normalized.startsWith("$2b$") || normalized.startsWith("$2y$");
    }

    private static boolean matchesLegacyDigest(String rawPassword, String username, String storedDigest) {
        String password = normalize(rawPassword);
        String userSalt = normalize(username);
        String pepperDigest = SmUtil.sm3(password + userSalt + pepper);
        if (pepperDigest.equals(storedDigest)) {
            return true;
        }
        String legacyDigest = SmUtil.sm3(password + userSalt);
        return legacyDigest.equals(storedDigest);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
