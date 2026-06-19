package com.ezboost.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DBConnection {

    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.apache.derby.client.ClientAutoloadedDriver");
        config.setJdbcUrl(AppConfig.databaseUrl());
        config.setUsername(AppConfig.databaseUser());
        config.setPassword(AppConfig.databasePassword());
        config.setPoolName("EzBoostPool");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        dataSource = new HikariDataSource(config);
        DatabaseMigration.ensureSchema(dataSource);
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void close() {
        if (!dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
