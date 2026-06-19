package com.ezboost.util;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtil {

    private PasswordUtil() {
    }

    public static String hashPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    public static boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null || storedPassword.trim().isEmpty()) {
            return false;
        }

        if (isHashed(storedPassword)) {
            return BCrypt.checkpw(rawPassword, storedPassword);
        }

        return storedPassword.trim().equals(rawPassword.trim());
    }

    public static boolean needsUpgrade(String storedPassword) {
        return storedPassword != null && !storedPassword.trim().isEmpty() && !isHashed(storedPassword);
    }

    public static String ensureHashed(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        return isHashed(password) ? password : hashPassword(password);
    }

    private static boolean isHashed(String password) {
        return password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
    }
}
