package com.ezboost.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppConfigTest {

    @AfterEach
    void clearProperties() {
        System.clearProperty("EZBOOST_DB_USER");
        System.clearProperty("EZBOOST_DB_PASSWORD");
    }

    @Test
    void requiresDatabaseCredentialsOutsideSourceCode() {
        assertThrows(IllegalStateException.class, AppConfig::databaseUser);
        assertThrows(IllegalStateException.class, AppConfig::databasePassword);

        System.setProperty("EZBOOST_DB_USER", "configured-user");
        System.setProperty("EZBOOST_DB_PASSWORD", "configured-password");

        assertEquals("configured-user", AppConfig.databaseUser());
        assertEquals("configured-password", AppConfig.databasePassword());
    }
}
