package com.gov.crypto;

import cn.hutool.crypto.SmUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 职责：验证统一密码加密组件的摘要与兼容校验逻辑。
 * 为什么存在：登录与用户管理都依赖该组件，任何回归都可能导致全站无法登录。
 */
class PasswordCryptoTest {

    @BeforeEach
    void setUp() {
        PasswordCrypto.configurePepper("");
    }

    /**
     * 作用：验证同一配置下生成的摘要可被正常匹配。
     */
    @Test
    void matches_shouldPassForCurrentDigest() {
        PasswordCrypto.configurePepper("pepper");
        String digest = PasswordCrypto.encode("secret", "tester");

        assertTrue(PasswordCrypto.matches("secret", "tester", digest));
    }

    /**
     * 作用：验证带 pepper 配置时仍能兼容历史无 pepper 摘要。
     */
    @Test
    void matches_shouldSupportLegacyDigest() {
        String legacyDigest = SmUtil.sm3("secret" + "tester");
        PasswordCrypto.configurePepper("new-pepper");

        assertTrue(PasswordCrypto.matches("secret", "tester", legacyDigest));
    }
}
