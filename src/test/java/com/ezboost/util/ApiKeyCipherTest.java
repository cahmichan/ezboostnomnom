package com.ezboost.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiKeyCipherTest {

    @AfterEach
    void clearKey() {
        System.clearProperty("EZBOOST_API_KEY_ENCRYPTION_KEY");
    }

    @Test
    void encryptsAndDecryptsApiKeysWithAesGcm() {
        byte[] key = new byte[32];
        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) i;
        }
        System.setProperty("EZBOOST_API_KEY_ENCRYPTION_KEY", Base64.getEncoder().encodeToString(key));

        String encrypted = ApiKeyCipher.encrypt("calendarific-test-key");

        assertTrue(ApiKeyCipher.isEncrypted(encrypted));
        assertNotEquals("calendarific-test-key", encrypted);
        assertEquals("calendarific-test-key", ApiKeyCipher.decrypt(encrypted));
    }

    @Test
    void acceptsLegacyPlaintextForReadCompatibility() {
        assertEquals("legacy-key", ApiKeyCipher.decrypt("legacy-key"));
    }
}
