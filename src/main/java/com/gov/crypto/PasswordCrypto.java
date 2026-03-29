package com.gov.crypto;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SmUtil;

/**
 * 职责：统一密码摘要与校验算法。
 * 为什么存在：避免控制器与服务层重复拼装摘要逻辑，并支持策略升级。
 * 关键输入输出：输入为明文密码、用户名，输出为 SM3 摘要或匹配结果。
 * 关联链路：/system/login、/system/user/add、/system/user/update。
 */
public final class PasswordCrypto {

    private static volatile String pepper = "";

    private PasswordCrypto() {
    }

    /**
     * 启动时注入环境级 pepper。
     */
    public static synchronized void configurePepper(String configuredPepper) {
        pepper = configuredPepper == null ? "" : configuredPepper.trim();
    }

    /**
     * 生成密码摘要。
     */
    public static String encode(String rawPassword, String username) {
        String password = normalize(rawPassword);
        String userSalt = normalize(username);
        return SmUtil.sm3(password + userSalt + pepper);
    }

    /**
     * 校验摘要是否匹配。
     * 兼容历史策略：若带 pepper 的摘要不匹配，回退校验旧版（无 pepper）摘要。
     */
    public static boolean matches(String rawPassword, String username, String storedDigest) {
        if (StrUtil.isBlank(storedDigest)) {
            return false;
        }
        String normalizedStoredDigest = storedDigest.trim();
        if (encode(rawPassword, username).equals(normalizedStoredDigest)) {
            return true;
        }
        String legacyDigest = SmUtil.sm3(normalize(rawPassword) + normalize(username));
        return legacyDigest.equals(normalizedStoredDigest);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
