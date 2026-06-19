package com.ezboost.util;

/**
 * Centralized, environment-aware application configuration. System properties
 * take precedence over environment variables so NetBeans and GlassFish can use
 * the same configuration keys without committing secrets to source control.
 */
public final class AppConfig {

    private AppConfig() {
    }

    public static String get(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            value = System.getenv(key);
        }
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(get(key, Boolean.toString(defaultValue)));
    }

    public static String getRequired(String key) {
        String value = get(key, "");
        if (value.isEmpty()) {
            throw new IllegalStateException("Missing required application configuration: " + key);
        }
        return value;
    }

    public static String databaseUrl() {
        return get("EZBOOST_DB_URL", "jdbc:derby://localhost:1527/ezboost_db");
    }

    public static String databaseUser() {
        return getRequired("EZBOOST_DB_USER");
    }

    public static String databasePassword() {
        return getRequired("EZBOOST_DB_PASSWORD");
    }

    public static boolean secureCookies() {
        return getBoolean("EZBOOST_SECURE_COOKIES", false);
    }

    public static boolean production() {
        return "production".equalsIgnoreCase(get("EZBOOST_ENV", "development"));
    }
}
