package com.gov.crypto;

import cn.hutool.crypto.SmUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordCryptoTest {

    @BeforeEach
    void setUp() {
        PasswordCrypto.configurePepper("");
    }

    @Test
    void matches_shouldPassForCurrentDigest() {
        PasswordCrypto.configurePepper("pepper");
        String digest = PasswordCrypto.encode("secret", "tester");

        assertTrue(PasswordCrypto.matches("secret", "tester", digest));
        assertTrue(PasswordCrypto.isBcryptDigest(digest));
        assertFalse(PasswordCrypto.needsUpgrade(digest));
    }

    @Test
    void matches_shouldSupportLegacyDigest() {
        String legacyDigest = SmUtil.sm3("secret" + "tester");
        PasswordCrypto.configurePepper("new-pepper");

        assertTrue(PasswordCrypto.matches("secret", "tester", legacyDigest));
        assertTrue(PasswordCrypto.needsUpgrade(legacyDigest));
    }
}
