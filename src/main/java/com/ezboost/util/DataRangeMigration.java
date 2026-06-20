package com.ezboost.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Applies check constraints only after proving existing data already satisfies them. */
final class DataRangeMigration {

    private DataRangeMigration() {
    }

    static void ensureConstraints(Connection connection) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        addCheckIfSafe(connection, metadata, "ACTUALROOMDATA", "CK_ROOM_VALID_RANGE",
                "BaseADR IS NOT NULL AND MinADR IS NOT NULL AND MaxADR IS NOT NULL AND Occupancy IS NOT NULL " +
                        "AND NumberOfRoom IS NOT NULL AND BaseADR > 0 AND MinADR > 0 AND MaxADR >= MinADR " +
                        "AND BaseADR >= MinADR AND BaseADR <= MaxADR AND Occupancy >= 0 AND Occupancy <= 100 " +
                        "AND NumberOfRoom > 0",
                "BaseADR IS NULL OR MinADR IS NULL OR MaxADR IS NULL OR Occupancy IS NULL OR NumberOfRoom IS NULL " +
                        "OR BaseADR <= 0 OR MinADR <= 0 OR MaxADR < MinADR OR BaseADR < MinADR OR BaseADR > MaxADR " +
                        "OR Occupancy < 0 OR Occupancy > 100 OR NumberOfRoom <= 0");
        addCheckIfSafe(connection, metadata, "MONTHLYSEASONDATA", "CK_MONTHLY_VALID_RANGE",
                "OccupancyRate IS NOT NULL AND TotalRevenue IS NOT NULL AND AvgRoomRate IS NOT NULL " +
                        "AND OccupancyRate >= 0 AND OccupancyRate <= 100 AND TotalRevenue >= 0 AND AvgRoomRate >= 0",
                "OccupancyRate IS NULL OR TotalRevenue IS NULL OR AvgRoomRate IS NULL OR OccupancyRate < 0 " +
                        "OR OccupancyRate > 100 OR TotalRevenue < 0 OR AvgRoomRate < 0");
        addCheckIfSafe(connection, metadata, "SEASONTHRESHOLD", "CK_THRESHOLD_ORDERED",
                "ThresholdLowNormal IS NOT NULL AND ThresholdNormalPeak IS NOT NULL AND ThresholdPeakSuperPeak IS NOT NULL " +
                        "AND ThresholdLowNormal >= 0 AND ThresholdLowNormal < ThresholdNormalPeak " +
                        "AND ThresholdNormalPeak < ThresholdPeakSuperPeak AND ThresholdPeakSuperPeak <= 100",
                "ThresholdLowNormal IS NULL OR ThresholdNormalPeak IS NULL OR ThresholdPeakSuperPeak IS NULL " +
                        "OR ThresholdLowNormal < 0 OR ThresholdLowNormal >= ThresholdNormalPeak " +
                        "OR ThresholdNormalPeak >= ThresholdPeakSuperPeak OR ThresholdPeakSuperPeak > 100");
        addCheckIfSafe(connection, metadata, "USERMULTIPLIERSETTINGS", "CK_MULTIPLIER_RANGE",
                "CustomMultiplier IS NOT NULL AND MinBound IS NOT NULL AND MaxBound IS NOT NULL " +
                        "AND MinBound > 0 AND MaxBound >= MinBound AND CustomMultiplier >= MinBound AND CustomMultiplier <= MaxBound",
                "CustomMultiplier IS NULL OR MinBound IS NULL OR MaxBound IS NULL OR MinBound <= 0 OR MaxBound < MinBound " +
                        "OR CustomMultiplier < MinBound OR CustomMultiplier > MaxBound");
        addCheckIfSafe(connection, metadata, "MARKETSEGMENT", "CK_SEGMENT_MULTIPLIER_RANGE",
                "rate_multiplier IS NOT NULL AND rate_multiplier >= 0.5 AND rate_multiplier <= 2.0",
                "rate_multiplier IS NULL OR rate_multiplier < 0.5 OR rate_multiplier > 2.0");
    }

    private static void addCheckIfSafe(Connection connection, DatabaseMetaData metadata, String tableName,
                                       String constraintName, String checkCondition, String invalidCondition)
            throws SQLException {
        if (!tableExists(metadata, tableName) || constraintExists(connection, tableName, constraintName)) return;
        try (Statement check = connection.createStatement();
             ResultSet invalid = check.executeQuery("SELECT 1 FROM " + quotedTable(tableName) + " WHERE " + invalidCondition + " FETCH FIRST 1 ROW ONLY")) {
            if (invalid.next()) {
                throw new SQLException("Cannot apply " + constraintName + ": invalid legacy range data was found in " +
                        tableName + ". Reconcile the records before restarting EzBoost.");
            }
        }
        try (Statement alter = connection.createStatement()) {
            alter.executeUpdate("ALTER TABLE " + quotedTable(tableName) + " ADD CONSTRAINT " + constraintName +
                    " CHECK (" + checkCondition + ")");
        }
    }

    private static boolean tableExists(DatabaseMetaData metadata, String tableName) throws SQLException {
        try (ResultSet tables = metadata.getTables(null, "APP", tableName, new String[]{"TABLE"})) {
            return tables.next();
        }
    }

    private static boolean constraintExists(Connection connection, String tableName, String constraintName) throws SQLException {
        String sql = "SELECT 1 FROM sys.sysconstraints c JOIN sys.systables t ON c.tableid = t.tableid " +
                "WHERE t.tablename = ? AND c.constraintname = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName.toUpperCase());
            statement.setString(2, constraintName.toUpperCase());
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private static String quotedTable(String tableName) {
        return "USER".equalsIgnoreCase(tableName) ? "\"USER\"" : tableName;
    }
}
