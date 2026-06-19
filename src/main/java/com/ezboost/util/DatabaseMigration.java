package com.ezboost.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

public final class DatabaseMigration {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigration.class);
    private static volatile boolean migrated;

    private DatabaseMigration() {
    }

    public static synchronized void ensureSchema(DataSource dataSource) {
        if (migrated) {
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            ensureMigrationHistory(conn);
            applyCurrentSchemaMigration(conn);
            migrated = true;
            logger.info("Database schema check complete");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to run database migrations", e);
        }
    }

    /**
     * Records the transition from legacy, hand-created Derby schemas to a
     * repeatable application migration history. Future changes must be added
     * as a new migration rather than changing an already recorded migration.
     */
    private static void applyCurrentSchemaMigration(Connection conn) throws SQLException {
        final String version = "001";
        if (!migrationApplied(conn, version)) {
            ensureUserOnboardingColumns(conn);
            ensureFutureEventTables(conn);
            ensureActualRoomDataColumns(conn);
            ensureMarketSegmentUserScopedConstraint(conn);
            recordMigration(conn, version, "baseline ownership and onboarding hardening");
            logger.info("Applied EzBoost schema migration {}", version);
        }

        final String auditVersion = "002";
        if (!migrationApplied(conn, auditVersion)) {
            ensureAuditAndOptimizationMetadataTables(conn);
            recordMigration(conn, auditVersion, "audit events and optimization run metadata");
            logger.info("Applied EzBoost schema migration {}", auditVersion);
        }
    }

    private static void ensureAuditAndOptimizationMetadataTables(Connection conn) throws SQLException {
        DatabaseMetaData metadata = conn.getMetaData();
        if (!tableExists(metadata, "AUDITEVENT")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE TABLE AuditEvent (" +
                        "event_id INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY, " +
                        "user_id INT NOT NULL, action VARCHAR(64) NOT NULL, entity_type VARCHAR(64) NOT NULL, " +
                        "outcome VARCHAR(32) NOT NULL, created_at TIMESTAMP NOT NULL)");
            }
        }
        if (!tableExists(metadata, "OPTIMIZATIONRUNMETADATA")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE TABLE OptimizationRunMetadata (" +
                        "request_id INT PRIMARY KEY, user_id INT NOT NULL, target_revenue DOUBLE NOT NULL, " +
                        "achieved_revenue DOUBLE NOT NULL, random_seed BIGINT NOT NULL, " +
                        "algorithm_version VARCHAR(64) NOT NULL, demand_curve_mode VARCHAR(64) NOT NULL, " +
                        "created_at TIMESTAMP NOT NULL)");
            }
        }
    }

    private static void ensureMigrationHistory(Connection conn) throws SQLException {
        if (tableExists(conn.getMetaData(), "EZBOOST_SCHEMA_HISTORY")) {
            return;
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE EzBoost_Schema_History (" +
                    "version VARCHAR(32) PRIMARY KEY, " +
                    "description VARCHAR(255) NOT NULL, " +
                    "applied_at TIMESTAMP NOT NULL)");
        }
        logger.info("Created EzBoost schema migration history");
    }

    private static boolean migrationApplied(Connection conn, String version) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT 1 FROM EzBoost_Schema_History WHERE version = ?")) {
            stmt.setString(1, version);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void recordMigration(Connection conn, String version, String description) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO EzBoost_Schema_History (version, description, applied_at) VALUES (?, ?, ?)") ) {
            stmt.setString(1, version);
            stmt.setString(2, description);
            stmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            stmt.executeUpdate();
        }
    }

    private static void ensureFutureEventTables(Connection conn) throws SQLException {
        if (!tableExists(conn.getMetaData(), "FUTUREEVENT")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE TABLE FutureEvent (" +
                        "event_id INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY, " +
                        "user_id INT NOT NULL, " +
                        "event_name VARCHAR(255) NOT NULL, " +
                        "event_date DATE NOT NULL, " +
                        "event_end_date DATE, " +
                        "event_type VARCHAR(50) NOT NULL, " +
                        "season_override VARCHAR(20) NOT NULL, " +
                        "source VARCHAR(50) NOT NULL, " +
                        "active BOOLEAN DEFAULT TRUE)");
            }
            logger.info("Created FutureEvent table");
        }

        if (!tableExists(conn.getMetaData(), "USERAPISETTINGS")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE TABLE UserApiSettings (" +
                        "setting_id INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY, " +
                        "user_id INT NOT NULL, " +
                        "setting_key VARCHAR(100) NOT NULL, " +
                        "setting_value VARCHAR(500), " +
                        "UNIQUE(user_id, setting_key))");
            }
            logger.info("Created UserApiSettings table");
        }
    }

    private static void ensureUserOnboardingColumns(Connection conn) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        if (!tableExists(metaData, "USER")) {
            return;
        }

        if (!columnExists(metaData, "USER", "ONBOARDING_REQUIRED")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("ALTER TABLE \"USER\" ADD COLUMN ONBOARDING_REQUIRED BOOLEAN DEFAULT FALSE");
            }
            logger.info("Added ONBOARDING_REQUIRED column to USER");
        }

        if (!columnExists(metaData, "USER", "ONBOARDING_COMPLETED")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("ALTER TABLE \"USER\" ADD COLUMN ONBOARDING_COMPLETED BOOLEAN DEFAULT TRUE");
            }
            logger.info("Added ONBOARDING_COMPLETED column to USER");
        }

        if (!columnExists(metaData, "USER", "ONBOARDING_STEP")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("ALTER TABLE \"USER\" ADD COLUMN ONBOARDING_STEP VARCHAR(32) DEFAULT 'COMPLETE'");
            }
            logger.info("Added ONBOARDING_STEP column to USER");
        }

        try (PreparedStatement defaults = conn.prepareStatement(
                "UPDATE \"USER\" SET ONBOARDING_REQUIRED = FALSE, ONBOARDING_COMPLETED = TRUE, " +
                        "ONBOARDING_STEP = 'COMPLETE' WHERE ONBOARDING_REQUIRED IS NULL " +
                        "OR ONBOARDING_COMPLETED IS NULL OR ONBOARDING_STEP IS NULL")) {
            defaults.executeUpdate();
        }
    }

    private static void ensureActualRoomDataColumns(Connection conn) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        if (!tableExists(metaData, "ACTUALROOMDATA")) {
            return;
        }

        if (!columnExists(metaData, "ACTUALROOMDATA", "USERID")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("ALTER TABLE ActualRoomData ADD COLUMN UserID INT");
            }
            logger.info("Added UserID column to ActualRoomData");
        }

        if (!columnExists(metaData, "ACTUALROOMDATA", "BASEADR")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("ALTER TABLE ActualRoomData ADD COLUMN BaseADR DOUBLE");
            }
            logger.info("Added BaseADR column to ActualRoomData");
        }

        if (!columnExists(metaData, "ACTUALROOMDATA", "BASEADRWASBACKFILLED")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("ALTER TABLE ActualRoomData ADD COLUMN BaseADRWasBackfilled BOOLEAN");
            }
            logger.info("Added BaseADRWasBackfilled column to ActualRoomData");
        }

        try (PreparedStatement markBackfilled = conn.prepareStatement(
                "UPDATE ActualRoomData SET BaseADRWasBackfilled = TRUE " +
                        "WHERE BaseADR IS NULL AND MinADR IS NOT NULL");
             PreparedStatement backfillBase = conn.prepareStatement(
                     "UPDATE ActualRoomData SET BaseADR = MinADR " +
                             "WHERE BaseADR IS NULL AND MinADR IS NOT NULL");
             PreparedStatement defaultBackfillFlag = conn.prepareStatement(
                     "UPDATE ActualRoomData SET BaseADRWasBackfilled = FALSE " +
                             "WHERE BaseADRWasBackfilled IS NULL");
             PreparedStatement assignLegacyUser = conn.prepareStatement(
                     "UPDATE ActualRoomData SET UserID = 0 WHERE UserID IS NULL")) {
            markBackfilled.executeUpdate();
            backfillBase.executeUpdate();
            defaultBackfillFlag.executeUpdate();
            assignLegacyUser.executeUpdate();
        }
    }

    private static void ensureMarketSegmentUserScopedConstraint(Connection conn) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        if (!tableExists(metaData, "MARKETSEGMENT")) {
            return;
        }

        if (!columnExists(metaData, "MARKETSEGMENT", "USER_ID")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("ALTER TABLE MarketSegment ADD COLUMN user_id INT");
            }
            logger.info("Added user_id column to MarketSegment");
        }

        try (PreparedStatement legacyUser = conn.prepareStatement(
                "UPDATE MarketSegment SET user_id = 1 WHERE user_id IS NULL")) {
            legacyUser.executeUpdate();
        }

        dropGlobalSegmentCodeUniqueConstraints(conn, metaData);

        if (!constraintExists(conn, "MARKETSEGMENT", "UQ_MARKET_SEGMENT_USER_CODE")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("ALTER TABLE MarketSegment ADD CONSTRAINT uq_market_segment_user_code " +
                        "UNIQUE (user_id, segment_code)");
            }
            logger.info("Added per-user unique constraint to MarketSegment(user_id, segment_code)");
        }
    }

    private static void dropGlobalSegmentCodeUniqueConstraints(Connection conn, DatabaseMetaData metaData) throws SQLException {
        List<String> globalUniqueNames = new java.util.ArrayList<>();
        try (ResultSet rs = metaData.getIndexInfo(null, "APP", "MARKETSEGMENT", true, false)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                String columnName = rs.getString("COLUMN_NAME");
                if (indexName != null && "SEGMENT_CODE".equalsIgnoreCase(columnName)
                        && isSingleColumnIndex(metaData, "MARKETSEGMENT", indexName)) {
                    globalUniqueNames.add(indexName);
                }
            }
        }

        for (String constraintName : globalUniqueNames) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("ALTER TABLE MarketSegment DROP CONSTRAINT \"" + constraintName + "\"");
                logger.info("Dropped global MarketSegment segment_code constraint {}", constraintName);
            } catch (SQLException e) {
                if (!"42X86".equals(e.getSQLState()) && !"42X65".equals(e.getSQLState())) {
                    throw e;
                }
            }
        }
    }

    private static boolean isSingleColumnIndex(DatabaseMetaData metaData, String tableName, String indexName)
            throws SQLException {
        int columnCount = 0;
        try (ResultSet rs = metaData.getIndexInfo(null, "APP", tableName, false, false)) {
            while (rs.next()) {
                if (indexName.equals(rs.getString("INDEX_NAME")) && rs.getString("COLUMN_NAME") != null) {
                    columnCount++;
                }
            }
        }
        return columnCount == 1;
    }

    private static boolean constraintExists(Connection conn, String tableName, String constraintName) throws SQLException {
        String sql = "SELECT 1 FROM sys.sysconstraints c " +
                "JOIN sys.systables t ON c.tableid = t.tableid " +
                "WHERE t.tablename = ? AND c.constraintname = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tableName.toUpperCase());
            stmt.setString(2, constraintName.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean tableExists(DatabaseMetaData metaData, String tableName) throws SQLException {
        try (ResultSet rs = metaData.getTables(null, "APP", tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private static boolean columnExists(DatabaseMetaData metaData, String tableName, String columnName) throws SQLException {
        try (ResultSet rs = metaData.getColumns(null, "APP", tableName, columnName)) {
            return rs.next();
        }
    }
}
