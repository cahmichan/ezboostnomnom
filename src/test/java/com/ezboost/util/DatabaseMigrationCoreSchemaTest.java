package com.ezboost.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseMigrationCoreSchemaTest {

    @BeforeEach
    @AfterEach
    void resetMigrationGuard() throws Exception {
        Field migrated = DatabaseMigration.class.getDeclaredField("migrated");
        migrated.setAccessible(true);
        migrated.setBoolean(null, false);
    }

    @Test
    void createsACompleteCoreSchemaForAnEmptyDatabase() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.apache.derby.iapi.jdbc.AutoloadedDriver");
        config.setJdbcUrl("jdbc:derby:memory:ezboost_core_schema_" + System.nanoTime() + ";create=true");
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(0);

        try (HikariDataSource dataSource = new HikariDataSource(config)) {
            DatabaseMigration.ensureSchema(dataSource);
            try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
                try (ResultSet history = statement.executeQuery("SELECT COUNT(*) FROM EzBoost_Schema_History")) {
                    history.next();
                    assertEquals(9, history.getInt(1));
                }
                DatabaseMetaData metadata = connection.getMetaData();
                for (String table : new String[]{"USER", "ACTUALROOMDATA", "MONTHLYSEASONDATA", "SEASONTHRESHOLD",
                        "USERMULTIPLIERSETTINGS", "MARKETSEGMENT", "OPTIMIZATIONREQUEST", "OPTIMIZATIONRESULT"}) {
                    try (ResultSet tables = metadata.getTables(null, "APP", table, new String[]{"TABLE"})) {
                        assertTrue(tables.next(), table + " should be created for a clean deployment");
                    }
                }
            }
        }
    }

    @Test
    void createsAndReconcilesSchemaForTheConfiguredDerbyAccount() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.apache.derby.iapi.jdbc.AutoloadedDriver");
        config.setJdbcUrl("jdbc:derby:memory:ezboost_schema_user_" + System.nanoTime() + ";create=true");
        config.setUsername("migrationowner");
        config.setPassword("test-password");
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(0);

        try (HikariDataSource dataSource = new HikariDataSource(config)) {
            DatabaseMigration.ensureSchema(dataSource);
            try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
                try (ResultSet history = statement.executeQuery("SELECT COUNT(*) FROM EzBoost_Schema_History")) {
                    history.next();
                    assertEquals(9, history.getInt(1));
                }
                try (ResultSet tables = connection.getMetaData().getTables(null, "MIGRATIONOWNER",
                        "EZBOOST_SCHEMA_HISTORY", new String[]{"TABLE"})) {
                    assertTrue(tables.next(), "migration history should use the configured Derby account schema");
                }
            }
        }
    }
}
