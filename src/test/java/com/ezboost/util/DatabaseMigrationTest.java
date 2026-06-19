package com.ezboost.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseMigrationTest {

    @Test
    void createsVersionedMigrationHistoryAndNewOperationalTables() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.apache.derby.iapi.jdbc.AutoloadedDriver");
        config.setJdbcUrl("jdbc:derby:memory:ezboost_migration_test;create=true");
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(0);

        try (HikariDataSource dataSource = new HikariDataSource(config)) {
            DatabaseMigration.ensureSchema(dataSource);
            try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
                try (ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM EzBoost_Schema_History")) {
                    rs.next();
                    assertEquals(4, rs.getInt(1));
                }
                DatabaseMetaData metadata = connection.getMetaData();
                int operationalTableCount = 0;
                for (String table : new String[]{"AUDITEVENT", "OPTIMIZATIONRUNMETADATA", "OPTIMIZATIONREPORTSNAPSHOT"}) {
                    try (ResultSet rs = metadata.getTables(null, "APP", table, new String[]{"TABLE"})) {
                        if (rs.next()) operationalTableCount++;
                    }
                }
                assertEquals(3, operationalTableCount);
            }
        }
    }
}
