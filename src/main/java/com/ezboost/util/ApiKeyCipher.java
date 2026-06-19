package com.ezboost.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/** Encrypts third-party credentials before they are written to Derby. */
public final class ApiKeyCipher {

    private static final String PREFIX = "enc:v1:";
    private static final int GCM_TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private ApiKeyCipher() {
    }

    public static boolean isConfigured() {
        String configured = AppConfig.get("EZBOOST_API_KEY_ENCRYPTION_KEY", "");
        return !configured.isEmpty();
    }

    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    public static String encrypt(String plaintext) {
        if (!isConfigured()) {
            throw new IllegalStateException("Calendarific key storage is not configured. "
                    + "Set EZBOOST_API_KEY_ENCRYPTION_KEY before saving an API key.");
        }
        try {
            byte[] iv = new byte[12];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Could not encrypt Calendarific API key", e);
        }
    }

    public static String decrypt(String storedValue) {
        if (storedValue == null || storedValue.trim().isEmpty()) {
            return storedValue;
        }
        if (!isEncrypted(storedValue)) {
            // Compatibility for existing records; the next explicit save encrypts the value.
            return storedValue;
        }
        if (!isConfigured()) {
            throw new IllegalStateException("Calendarific key encryption is configured in the database, "
                    + "but EZBOOST_API_KEY_ENCRYPTION_KEY is unavailable.");
        }
        try {
            byte[] payload = Base64.getDecoder().decode(storedValue.substring(PREFIX.length()));
            if (payload.length <= 12) {
                throw new IllegalArgumentException("Invalid encrypted API key payload");
            }
            byte[] iv = java.util.Arrays.copyOfRange(payload, 0, 12);
            byte[] encrypted = java.util.Arrays.copyOfRange(payload, 12, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Could not decrypt Calendarific API key", e);
        }
    }

    private static SecretKeySpec key() {
        try {
            byte[] key = Base64.getDecoder().decode(AppConfig.get("EZBOOST_API_KEY_ENCRYPTION_KEY", ""));
            if (key.length != 32) {
                throw new IllegalArgumentException("EZBOOST_API_KEY_ENCRYPTION_KEY must decode to exactly 32 bytes");
            }
            return new SecretKeySpec(key, "AES");
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid EZBOOST_API_KEY_ENCRYPTION_KEY", e);
        }
    }
}
