package com.ezboost.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseMigrationReconciliationTest {

    @BeforeEach
    @AfterEach
    void resetMigrationGuard() throws Exception {
        Field migrated = DatabaseMigration.class.getDeclaredField("migrated");
        migrated.setAccessible(true);
        migrated.setBoolean(null, false);
    }

    @Test
    void stopsWhenLegacyCanonicalEmailsAreDuplicated() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.apache.derby.iapi.jdbc.AutoloadedDriver");
        config.setJdbcUrl("jdbc:derby:memory:ezboost_duplicate_migration_" + System.nanoTime() + ";create=true");
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(0);

        try (HikariDataSource dataSource = new HikariDataSource(config)) {
            try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE \"USER\" (UserID INT PRIMARY KEY, Username VARCHAR(255), Email VARCHAR(320))");
                statement.executeUpdate("INSERT INTO \"USER\" VALUES (1, 'first', 'duplicate@example.com')");
                statement.executeUpdate("INSERT INTO \"USER\" VALUES (2, 'second', ' DUPLICATE@example.com ')");
            }

            IllegalStateException failure = assertThrows(IllegalStateException.class,
                    () -> DatabaseMigration.ensureSchema(dataSource));

            assertTrue(failure.getCause().getMessage().contains("UQ_USER_EMAIL_KEY"));
            assertTrue(failure.getCause().getMessage().contains("Resolve duplicates"));
        }
    }
}
