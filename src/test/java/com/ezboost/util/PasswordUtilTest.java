package com.ezboost.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    void testHashPasswordAndMatch() {
        String hashed = PasswordUtil.hashPassword("secret123");

        assertNotEquals("secret123", hashed);
        assertTrue(PasswordUtil.matches("secret123", hashed));
        assertFalse(PasswordUtil.matches("wrong", hashed));
    }

    @Test
    void testLegacyPasswordMatchesAndNeedsUpgrade() {
        assertTrue(PasswordUtil.matches("legacy-pass", "legacy-pass"));
        assertTrue(PasswordUtil.needsUpgrade("legacy-pass"));
    }

    @Test
    void testEnsureHashedLeavesHashedValueUntouched() {
        String hashed = PasswordUtil.hashPassword("keepme");
        assertEquals(hashed, PasswordUtil.ensureHashed(hashed));
    }
}
