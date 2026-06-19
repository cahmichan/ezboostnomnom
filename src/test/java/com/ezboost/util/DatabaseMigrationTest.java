package com.ezboost.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabaseMigrationTest {

    @Test
    void createsVersionedMigrationHistoryAndNewOperationalTables() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.apache.derby.iapi.jdbc.AutoloadedDriver");
        config.setJdbcUrl("jdbc:derby:memory:ezboost_migration_test;create=true");
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(0);

        try (HikariDataSource dataSource = new HikariDataSource(config)) {
            createLegacySchema(dataSource);
            DatabaseMigration.ensureSchema(dataSource);
            try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
                try (ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM EzBoost_Schema_History")) {
                    rs.next();
                    assertEquals(6, rs.getInt(1));
                }
                DatabaseMetaData metadata = connection.getMetaData();
                int operationalTableCount = 0;
                for (String table : new String[]{"AUDITEVENT", "OPTIMIZATIONRUNMETADATA", "OPTIMIZATIONREPORTSNAPSHOT"}) {
                    try (ResultSet rs = metadata.getTables(null, "APP", table, new String[]{"TABLE"})) {
                        if (rs.next()) operationalTableCount++;
                    }
                }
                assertEquals(3, operationalTableCount);

                try (ResultSet rs = statement.executeQuery(
                        "SELECT EMAIL_KEY, USERNAME_KEY FROM \"USER\" WHERE UserID = 1")) {
                    rs.next();
                    assertEquals("owner@example.com", rs.getString("EMAIL_KEY"));
                    assertEquals("owner", rs.getString("USERNAME_KEY"));
                }

                assertThrows(java.sql.SQLException.class, () -> statement.executeUpdate(
                        "INSERT INTO ActualRoomData (UserID, RoomType, MinADR) VALUES (1, 'Deluxe', 250.0)"));
                assertThrows(java.sql.SQLException.class, () -> statement.executeUpdate(
                        "INSERT INTO MonthlySeasonData (UserID, MonthYear) VALUES (1, '2024-01')"));
            }
        }
    }

    private void createLegacySchema(HikariDataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE \"USER\" (UserID INT PRIMARY KEY, FirstName VARCHAR(50), " +
                    "LastName VARCHAR(50), Username VARCHAR(255), Email VARCHAR(320), Password VARCHAR(255), PhoneNumber VARCHAR(30))");
            statement.executeUpdate("INSERT INTO \"USER\" VALUES (1, 'Owner', 'One', ' Owner ', ' Owner@Example.com ', 'hash', '123')");
            statement.executeUpdate("CREATE TABLE ActualRoomData (RoomDataID INT PRIMARY KEY, UserID INT, RoomType VARCHAR(100), MinADR DOUBLE)");
            statement.executeUpdate("INSERT INTO ActualRoomData VALUES (1, 1, 'Deluxe', 200.0)");
            statement.executeUpdate("CREATE TABLE MonthlySeasonData (DataID INT PRIMARY KEY, UserID INT, MonthYear VARCHAR(7))");
            statement.executeUpdate("INSERT INTO MonthlySeasonData VALUES (1, 1, '2024-01')");
            statement.executeUpdate("CREATE TABLE SeasonThreshold (ThresholdID INT PRIMARY KEY, UserID INT)");
            statement.executeUpdate("INSERT INTO SeasonThreshold VALUES (1, 1)");
            statement.executeUpdate("CREATE TABLE OptimizationRequest (RequestID INT PRIMARY KEY, UserID INT)");
        }
    }
}
