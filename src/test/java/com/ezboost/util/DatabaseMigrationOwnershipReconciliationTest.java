package com.ezboost.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseMigrationOwnershipReconciliationTest {

    @BeforeEach
    @AfterEach
    void resetMigrationGuard() throws Exception {
        Field migrated = DatabaseMigration.class.getDeclaredField("migrated");
        migrated.setAccessible(true);
        migrated.setBoolean(null, false);
    }

    @Test
    void stopsBeforeAssigningAnUnownedLegacyRoomToASyntheticUser() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.apache.derby.iapi.jdbc.AutoloadedDriver");
        config.setJdbcUrl("jdbc:derby:memory:ezboost_unowned_room_" + System.nanoTime() + ";create=true");
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(0);

        try (HikariDataSource dataSource = new HikariDataSource(config)) {
            try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE \"USER\" (UserID INT PRIMARY KEY, Username VARCHAR(255), Email VARCHAR(320))");
                statement.executeUpdate("INSERT INTO \"USER\" VALUES (1, 'owner', 'owner@example.com')");
                statement.executeUpdate("CREATE TABLE ActualRoomData (RoomDataID INT PRIMARY KEY, RoomType VARCHAR(100), MinADR DOUBLE)");
                statement.executeUpdate("INSERT INTO ActualRoomData VALUES (1, 'Deluxe', 200.0)");
            }

            IllegalStateException failure = assertThrows(IllegalStateException.class,
                    () -> DatabaseMigration.ensureSchema(dataSource));
            assertTrue(failure.getCause().getMessage().contains("no user owner"));

            try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery("SELECT UserID FROM ActualRoomData WHERE RoomDataID = 1")) {
                rows.next();
                assertEquals(0, rows.getInt(1));
                assertTrue(rows.wasNull());
            }
        }
    }
}
