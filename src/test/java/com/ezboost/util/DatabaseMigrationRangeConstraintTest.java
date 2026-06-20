package com.ezboost.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabaseMigrationRangeConstraintTest {

    @BeforeEach
    @AfterEach
    void resetMigrationGuard() throws Exception {
        Field migrated = DatabaseMigration.class.getDeclaredField("migrated");
        migrated.setAccessible(true);
        migrated.setBoolean(null, false);
    }

    @Test
    void enforcesRoomPricingAndOccupancyRangesAfterMigration() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.apache.derby.iapi.jdbc.AutoloadedDriver");
        config.setJdbcUrl("jdbc:derby:memory:ezboost_range_schema_" + System.nanoTime() + ";create=true");
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(0);

        try (HikariDataSource dataSource = new HikariDataSource(config)) {
            DatabaseMigration.ensureSchema(dataSource);
            try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
                statement.executeUpdate("INSERT INTO \"USER\" (FirstName, LastName, Username, Email, Password, PhoneNumber, Email_Key, Username_Key) " +
                        "VALUES ('Owner', 'One', 'owner', 'owner@example.com', 'hash', '1234567', 'owner@example.com', 'owner')");
                assertThrows(SQLException.class, () -> statement.executeUpdate(
                        "INSERT INTO ActualRoomData (UserID, RoomType, BaseADR, BaseADRWasBackfilled, MinADR, MaxADR, Occupancy, NumberOfRoom) " +
                                "VALUES (1, 'Deluxe', 250, FALSE, 300, 200, 75, 10)"));
            }
        }
    }
}
