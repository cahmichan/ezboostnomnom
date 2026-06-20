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

        final String snapshotVersion = "003";
        if (!migrationApplied(conn, snapshotVersion)) {
            ensureOptimizationReportSnapshotTable(conn);
            recordMigration(conn, snapshotVersion, "durable optimization report snapshots");
            logger.info("Applied EzBoost schema migration {}", snapshotVersion);
        }

        final String integrityVersion = "004";
        if (!migrationApplied(conn, integrityVersion)) {
            ensureDataIntegrityConstraints(conn);
            recordMigration(conn, integrityVersion, "canonical identity and per-user data integrity constraints");
            logger.info("Applied EzBoost schema migration {}", integrityVersion);
        }

        final String ownershipVersion = "005";
        if (!migrationApplied(conn, ownershipVersion)) {
            ensureOwnershipForeignKeys(conn);
            recordMigration(conn, ownershipVersion, "owner foreign keys for user-scoped records");
            logger.info("Applied EzBoost schema migration {}", ownershipVersion);
        }

        final String multiplierOwnershipVersion = "006";
        if (!migrationApplied(conn, multiplierOwnershipVersion)) {
            ensureMultiplierOwnershipForeignKey(conn);
            recordMigration(conn, multiplierOwnershipVersion, "owner foreign key for multiplier settings");
            logger.info("Applied EzBoost schema migration {}", multiplierOwnershipVersion);
        }
        final String coreSchemaVersion = "007";
        if (!migrationApplied(conn, coreSchemaVersion)) {
            CoreSchemaMigration.ensureCoreTables(conn);
            ensureMarketSegmentUserScopedConstraint(conn);
            ensureDataIntegrityConstraints(conn);
            ensureOwnershipForeignKeys(conn);
            ensureMultiplierOwnershipForeignKey(conn);
            recordMigration(conn, coreSchemaVersion, "baseline core schema for clean deployments");
            logger.info("Applied EzBoost schema migration {}", coreSchemaVersion);
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

    private static void ensureOptimizationReportSnapshotTable(Connection conn) throws SQLException {
        if (tableExists(conn.getMetaData(), "OPTIMIZATIONREPORTSNAPSHOT")) {
            return;
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE OptimizationReportSnapshot (" +
                    "request_id INT PRIMARY KEY, user_id INT NOT NULL, payload CLOB NOT NULL, " +
                    "created_at TIMESTAMP NOT NULL)");
        }
    }

    private static void ensureDataIntegrityConstraints(Connection conn) throws SQLException {
        DatabaseMetaData metadata = conn.getMetaData();
        ensureCanonicalUserIdentity(conn, metadata);
        addUniqueConstraintIfSafe(conn, metadata, "ACTUALROOMDATA", "UQ_ROOM_USER_TYPE", "UserID, RoomType");
        addUniqueConstraintIfSafe(conn, metadata, "MONTHLYSEASONDATA", "UQ_MONTHLY_USER_PERIOD", "UserID, MonthYear");
        addUniqueConstraintIfSafe(conn, metadata, "SEASONTHRESHOLD", "UQ_THRESHOLD_USER", "UserID");
        ensureIndexes(conn, metadata);
    }

    /**
     * Makes ownership enforced by Derby. Orphans are a migration failure: they
     * must be reconciled deliberately rather than reassigned or deleted.
     */
    private static void ensureOwnershipForeignKeys(Connection conn) throws SQLException {
        DatabaseMetaData metadata = conn.getMetaData();
        if (!tableExists(metadata, "USER")) return;

        addForeignKeyIfSafe(conn, metadata, "ACTUALROOMDATA", "USERID", "FK_ROOM_USER");
        addForeignKeyIfSafe(conn, metadata, "MONTHLYSEASONDATA", "USERID", "FK_MONTHLY_USER");
        addForeignKeyIfSafe(conn, metadata, "SEASONTHRESHOLD", "USERID", "FK_THRESHOLD_USER");
        addForeignKeyIfSafe(conn, metadata, "OPTIMIZATIONREQUEST", "USERID", "FK_REQUEST_USER");
        addForeignKeyIfSafe(conn, metadata, "MARKETSEGMENT", "USER_ID", "FK_SEGMENT_USER");
        addForeignKeyIfSafe(conn, metadata, "FUTUREEVENT", "USER_ID", "FK_EVENT_USER");
        addForeignKeyIfSafe(conn, metadata, "USERAPISETTINGS", "USER_ID", "FK_API_SETTING_USER");
        addForeignKeyIfSafe(conn, metadata, "AUDITEVENT", "USER_ID", "FK_AUDIT_USER");
        addForeignKeyIfSafe(conn, metadata, "OPTIMIZATIONRUNMETADATA", "USER_ID", "FK_RUN_METADATA_USER");
        addForeignKeyIfSafe(conn, metadata, "OPTIMIZATIONREPORTSNAPSHOT", "USER_ID", "FK_REPORT_SNAPSHOT_USER");
    }

    private static void ensureMultiplierOwnershipForeignKey(Connection conn) throws SQLException {
        addForeignKeyIfSafe(conn, conn.getMetaData(), "USERMULTIPLIERSETTINGS", "USERID", "FK_MULTIPLIER_USER");
    }

    private static void addForeignKeyIfSafe(Connection conn, DatabaseMetaData metadata, String tableName,
                                            String columnName, String constraintName) throws SQLException {
        if (!tableExists(metadata, tableName) || !columnExists(metadata, tableName, columnName)
                || constraintExists(conn, tableName, constraintName)) {
            return;
        }
        String orphanSql = "SELECT 1 FROM " + quotedTable(tableName) + " child LEFT JOIN \"USER\" owner " +
                "ON child." + columnName + " = owner.UserID WHERE child." + columnName +
                " IS NOT NULL AND owner.UserID IS NULL FETCH FIRST 1 ROW ONLY";
        try (Statement orphanCheck = conn.createStatement(); ResultSet orphans = orphanCheck.executeQuery(orphanSql)) {
            if (orphans.next()) {
                throw new SQLException("Cannot apply " + constraintName + ": orphaned " + columnName +
                        " values were found in " + tableName + ". Reconcile the records before restarting EzBoost.");
            }
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("ALTER TABLE " + quotedTable(tableName) + " ADD CONSTRAINT " + constraintName +
                    " FOREIGN KEY (" + columnName + ") REFERENCES \"USER\" (UserID)");
        }
    }

    private static void ensureCanonicalUserIdentity(Connection conn, DatabaseMetaData metadata) throws SQLException {
        if (!tableExists(metadata, "USER")) return;
        if (!columnExists(metadata, "USER", "EMAIL_KEY")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("ALTER TABLE \"USER\" ADD COLUMN EMAIL_KEY VARCHAR(320)");
            }
        }
        if (!columnExists(metadata, "USER", "USERNAME_KEY")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("ALTER TABLE \"USER\" ADD COLUMN USERNAME_KEY VARCHAR(255)");
            }
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE \"USER\" SET EMAIL_KEY = LOWER(TRIM(EMAIL)), USERNAME_KEY = LOWER(TRIM(USERNAME)) " +
                    "WHERE EMAIL_KEY IS NULL OR USERNAME_KEY IS NULL");
        }
        addUniqueConstraintIfSafe(conn, metadata, "USER", "UQ_USER_EMAIL_KEY", "EMAIL_KEY");
        addUniqueConstraintIfSafe(conn, metadata, "USER", "UQ_USER_USERNAME_KEY", "USERNAME_KEY");
    }

    private static void addUniqueConstraintIfSafe(Connection conn, DatabaseMetaData metadata, String tableName,
                                                   String constraintName, String columns) throws SQLException {
        if (!tableExists(metadata, tableName) || constraintExists(conn, tableName, constraintName)) return;
        String duplicateSql = "SELECT " + columns + ", COUNT(*) FROM " + quotedTable(tableName) +
                " GROUP BY " + columns + " HAVING COUNT(*) > 1 FETCH FIRST 1 ROW ONLY";
        try (Statement duplicateCheck = conn.createStatement(); ResultSet duplicates = duplicateCheck.executeQuery(duplicateSql)) {
            if (duplicates.next()) {
                throw new SQLException("Cannot apply " + constraintName + ": duplicate existing data was found in " +
                        tableName + ". Resolve duplicates before restarting EzBoost.");
            }
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("ALTER TABLE " + quotedTable(tableName) + " ADD CONSTRAINT " + constraintName +
                    " UNIQUE (" + columns + ")");
        }
    }

    private static void ensureIndexes(Connection conn, DatabaseMetaData metadata) throws SQLException {
        createIndexIfMissing(conn, metadata, "ACTUALROOMDATA", "IDX_ROOM_USER", "UserID");
        createIndexIfMissing(conn, metadata, "MONTHLYSEASONDATA", "IDX_MONTHLY_USER", "UserID");
        createIndexIfMissing(conn, metadata, "FUTUREEVENT", "IDX_EVENT_USER_DATE", "user_id, event_date");
        createIndexIfMissing(conn, metadata, "OPTIMIZATIONREQUEST", "IDX_REQUEST_USER", "UserID");
    }

    private static void createIndexIfMissing(Connection conn, DatabaseMetaData metadata, String tableName,
                                             String indexName, String columns) throws SQLException {
        if (!tableExists(metadata, tableName) || indexExists(metadata, tableName, indexName)) return;
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE INDEX " + indexName + " ON " + quotedTable(tableName) + " (" + columns + ")");
        }
    }

    private static boolean indexExists(DatabaseMetaData metadata, String tableName, String indexName) throws SQLException {
        try (ResultSet rs = metadata.getIndexInfo(null, "APP", tableName, false, false)) {
            while (rs.next()) {
                if (indexName.equalsIgnoreCase(rs.getString("INDEX_NAME"))) return true;
            }
        }
        return false;
    }

    private static String quotedTable(String tableName) {
        return "USER".equalsIgnoreCase(tableName) ? "\"USER\"" : tableName;
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

        if (hasRowsWithNullUserId(conn, "ActualRoomData")) {
            throw new SQLException("Cannot migrate ActualRoomData: one or more legacy room records have no user owner. Reconcile ownership before restarting EzBoost.");
        }
        try (PreparedStatement markBackfilled = conn.prepareStatement(
                "UPDATE ActualRoomData SET BaseADRWasBackfilled = TRUE " +
                        "WHERE BaseADR IS NULL AND MinADR IS NOT NULL");
             PreparedStatement backfillBase = conn.prepareStatement(
                     "UPDATE ActualRoomData SET BaseADR = MinADR " +
                             "WHERE BaseADR IS NULL AND MinADR IS NOT NULL");
             PreparedStatement defaultBackfillFlag = conn.prepareStatement(
                     "UPDATE ActualRoomData SET BaseADRWasBackfilled = FALSE " +
                             "WHERE BaseADRWasBackfilled IS NULL")) {
            markBackfilled.executeUpdate();
            backfillBase.executeUpdate();
            defaultBackfillFlag.executeUpdate();
        }
    }

    private static boolean hasRowsWithNullUserId(Connection conn, String tableName) throws SQLException {
        try (Statement statement = conn.createStatement();
             ResultSet rows = statement.executeQuery("SELECT 1 FROM " + tableName + " WHERE UserID IS NULL FETCH FIRST 1 ROW ONLY")) {
            return rows.next();
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
