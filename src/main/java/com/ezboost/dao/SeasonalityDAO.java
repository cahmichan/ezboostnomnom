package com.ezboost.dao;

import com.ezboost.model.MonthlySeasonData;
import com.ezboost.model.Season;
import com.ezboost.model.SeasonThreshold;
import com.ezboost.util.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SeasonalityDAO - Database operations for monthly data, seasonality, and thresholds
 * Enhancement i: Data-driven seasonality from CSV import with GA classification
 */
public class SeasonalityDAO {

    private static final Logger logger = LoggerFactory.getLogger(SeasonalityDAO.class);

    // ==================== MONTHLY DATA OPERATIONS ====================

    /**
     * Save monthly season data (from CSV import)
     */
    public static boolean saveMonthlyData(MonthlySeasonData data) {
        String sql = "INSERT INTO MonthlySeasonData (UserID, MonthYear, MonthName, " +
                     "OccupancyRate, TotalRevenue, AvgRoomRate, ClassifiedSeason) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, data.getUserId());
            pstmt.setString(2, data.getMonthYear());
            pstmt.setString(3, data.getMonthName());
            pstmt.setDouble(4, data.getOccupancyRate());
            pstmt.setDouble(5, data.getTotalRevenue());
            pstmt.setDouble(6, data.getAvgRoomRate());
            pstmt.setString(7, data.getClassifiedSeason());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error saving monthly data", e);
        }
        return false;
    }

    /**
     * Batch save multiple months of data (for CSV import)
     */
    public static int batchSaveMonthlyData(List<MonthlySeasonData> dataList) {
        String sql = "INSERT INTO MonthlySeasonData (UserID, MonthYear, MonthName, " +
                     "OccupancyRate, TotalRevenue, AvgRoomRate, ClassifiedSeason) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        int successCount = 0;

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (MonthlySeasonData data : dataList) {
                    pstmt.setInt(1, data.getUserId());
                    pstmt.setString(2, data.getMonthYear());
                    pstmt.setString(3, data.getMonthName());
                    pstmt.setDouble(4, data.getOccupancyRate());
                    pstmt.setDouble(5, data.getTotalRevenue());
                    pstmt.setDouble(6, data.getAvgRoomRate());
                    pstmt.setString(7, data.getClassifiedSeason());
                    pstmt.addBatch();
                }
                int[] results = pstmt.executeBatch();
                for (int result : results) {
                    if (result > 0 || result == Statement.SUCCESS_NO_INFO) successCount++;
                }
            }
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            logger.error("Error in batch save", e);
        } finally {
            if (conn != null) { try { conn.close(); } catch (SQLException ignored) {} }
        }
        return successCount;
    }

    /**
     * Get all monthly data for a user
     */
    public static List<MonthlySeasonData> getMonthlyDataByUser(int userId) {
        List<MonthlySeasonData> dataList = new ArrayList<>();
        String sql = "SELECT * FROM MonthlySeasonData WHERE UserID = ? ORDER BY MonthYear";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                dataList.add(mapResultSetToMonthlyData(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching monthly data", e);
        }
        return dataList;
    }

    /**
     * Get monthly data for a specific year
     */
    public static List<MonthlySeasonData> getMonthlyDataByYear(int userId, String year) {
        List<MonthlySeasonData> dataList = new ArrayList<>();
        String sql = "SELECT * FROM MonthlySeasonData WHERE UserID = ? AND MonthYear LIKE ? ORDER BY MonthYear";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setString(2, year + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                dataList.add(mapResultSetToMonthlyData(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching yearly data", e);
        }
        return dataList;
    }

    /**
     * Get season distribution (count of months in each season)
     */
    public static Map<String, Integer> getSeasonDistribution(int userId) {
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("LOW", 0);
        distribution.put("NORMAL", 0);
        distribution.put("PEAK", 0);
        distribution.put("SUPER_PEAK", 0);

        String sql = "SELECT ClassifiedSeason, COUNT(*) as Count FROM MonthlySeasonData " +
                     "WHERE UserID = ? GROUP BY ClassifiedSeason";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String season = rs.getString("ClassifiedSeason");
                if (season != null) {
                    distribution.put(season, rs.getInt("Count"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching season distribution", e);
        }
        return distribution;
    }

    /**
     * Get average metrics by season
     */
    public static Map<String, double[]> getAverageMetricsBySeason(int userId) {
        Map<String, double[]> metrics = new HashMap<>();
        String sql = "SELECT ClassifiedSeason, AVG(OccupancyRate) as AvgOcc, " +
                     "AVG(TotalRevenue) as AvgRev, AVG(AvgRoomRate) as AvgADR " +
                     "FROM MonthlySeasonData WHERE UserID = ? GROUP BY ClassifiedSeason";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String season = rs.getString("ClassifiedSeason");
                if (season != null) {
                    double[] values = new double[3];
                    values[0] = rs.getDouble("AvgOcc");
                    values[1] = rs.getDouble("AvgRev");
                    values[2] = rs.getDouble("AvgADR");
                    metrics.put(season, values);
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching average metrics", e);
        }
        return metrics;
    }

    // ==================== DELETE OPERATIONS ====================

    /**
     * DELETE: Delete a single month's data by ID
     */
    public static boolean deleteMonthlyDataById(int dataId) {
        String sql = "DELETE FROM MonthlySeasonData WHERE DataID = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, dataId);
            int affected = pstmt.executeUpdate();
            logger.debug("Deleted monthly data ID: {}", dataId);
            return affected > 0;
        } catch (SQLException e) {
            logger.error("Error deleting monthly data", e);
        }
        return false;
    }

    /**
     * DELETE: Delete all monthly data for a user
     */
    public static boolean deleteAllUserMonthlyData(int userId) {
        String sql = "DELETE FROM MonthlySeasonData WHERE UserID = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            int affected = pstmt.executeUpdate();
            logger.debug("Deleted all monthly data for user: {} ({} records)", userId, affected);
            return true;
        } catch (SQLException e) {
            logger.error("Error deleting all monthly data", e);
        }
        return false;
    }

    /**
     * DELETE: Delete monthly data for a specific year
     */
    public static boolean deleteMonthlyDataByYear(int userId, String year) {
        String sql = "DELETE FROM MonthlySeasonData WHERE UserID = ? AND MonthYear LIKE ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setString(2, year + "%");
            int affected = pstmt.executeUpdate();
            logger.debug("Deleted {} data for user: {} ({} records)", year, userId, affected);
            return true;
        } catch (SQLException e) {
            logger.error("Error deleting yearly data", e);
        }
        return false;
    }

    /**
     * Update season classification for a month
     */
    public static boolean updateMonthSeason(int dataId, String newSeason) {
        String sql = "UPDATE MonthlySeasonData SET ClassifiedSeason = ? WHERE DataID = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newSeason);
            pstmt.setInt(2, dataId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating month season", e);
        }
        return false;
    }

    /**
     * Update all seasons for a user based on new thresholds
     */
    public static boolean reclassifyAllSeasons(int userId, SeasonThreshold thresholds) {
        List<MonthlySeasonData> dataList = getMonthlyDataByUser(userId);
        String sql = "UPDATE MonthlySeasonData SET ClassifiedSeason = ? WHERE DataID = ?";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (MonthlySeasonData data : dataList) {
                    String newSeason = thresholds.classifyOccupancy(data.getOccupancyRate());
                    pstmt.setString(1, newSeason);
                    pstmt.setInt(2, data.getDataId());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            logger.error("Error reclassifying seasons", e);
        } finally {
            if (conn != null) { try { conn.close(); } catch (SQLException ignored) {} }
        }
        return false;
    }

    // ==================== THRESHOLD OPERATIONS ====================

    /**
     * Save or update user's season thresholds
     */
    public static int saveThresholds(SeasonThreshold threshold) {
        SeasonThreshold existing = getThresholdsByUser(threshold.getUserId());

        if (existing != null) {
            return updateThresholds(threshold) ? existing.getThresholdId() : -1;
        }

        String sql = "INSERT INTO SeasonThreshold (UserID, ThresholdLowNormal, ThresholdNormalPeak, " +
                     "ThresholdPeakSuperPeak, IsAutoGenerated) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, threshold.getUserId());
            pstmt.setDouble(2, threshold.getThresholdLowNormal());
            pstmt.setDouble(3, threshold.getThresholdNormalPeak());
            pstmt.setDouble(4, threshold.getThresholdPeakSuperPeak());
            pstmt.setBoolean(5, threshold.isAutoGenerated());

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                ResultSet keys = pstmt.getGeneratedKeys();
                if (keys.next()) {
                    int id = keys.getInt(1);
                    logger.debug("Saved new thresholds ID: {}", id);
                    return id;
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving thresholds", e);
        }
        return -1;
    }

    /**
     * Update existing thresholds
     */
    public static boolean updateThresholds(SeasonThreshold threshold) {
        String sql = "UPDATE SeasonThreshold SET ThresholdLowNormal = ?, ThresholdNormalPeak = ?, " +
                     "ThresholdPeakSuperPeak = ?, IsAutoGenerated = ?, LastUpdated = CURRENT_TIMESTAMP " +
                     "WHERE UserID = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, threshold.getThresholdLowNormal());
            pstmt.setDouble(2, threshold.getThresholdNormalPeak());
            pstmt.setDouble(3, threshold.getThresholdPeakSuperPeak());
            pstmt.setBoolean(4, threshold.isAutoGenerated());
            pstmt.setInt(5, threshold.getUserId());

            boolean success = pstmt.executeUpdate() > 0;
            logger.debug("Updated thresholds for user: {}", threshold.getUserId());
            return success;
        } catch (SQLException e) {
            logger.error("Error updating thresholds", e);
        }
        return false;
    }

    /**
     * Get thresholds for a user
     */
    public static SeasonThreshold getThresholdsByUser(int userId) {
        String sql = "SELECT * FROM SeasonThreshold WHERE UserID = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToThreshold(rs);
            }
        } catch (SQLException e) {
            logger.error("Error fetching thresholds", e);
        }
        return null;
    }

    /**
     * Delete user's thresholds
     */
    public static boolean deleteThresholds(int userId) {
        String sql = "DELETE FROM SeasonThreshold WHERE UserID = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
            logger.debug("Deleted thresholds for user: {}", userId);
            return true;
        } catch (SQLException e) {
            logger.error("Error deleting thresholds", e);
        }
        return false;
    }

    /**
     * Get or create default thresholds for a user
     */
    public static SeasonThreshold getOrCreateThresholds(int userId) {
        SeasonThreshold existing = getThresholdsByUser(userId);
        if (existing != null) {
            return existing;
        }

        SeasonThreshold defaults = new SeasonThreshold(userId, 65.0, 75.0, 85.0, false);
        saveThresholds(defaults);
        return defaults;
    }

    // ==================== HELPER METHODS ====================

    private static MonthlySeasonData mapResultSetToMonthlyData(ResultSet rs) throws SQLException {
        MonthlySeasonData data = new MonthlySeasonData();
        data.setDataId(rs.getInt("DataID"));
        data.setUserId(rs.getInt("UserID"));
        data.setMonthYear(rs.getString("MonthYear"));
        data.setMonthName(rs.getString("MonthName"));
        data.setOccupancyRate(rs.getDouble("OccupancyRate"));
        data.setTotalRevenue(rs.getDouble("TotalRevenue"));
        data.setAvgRoomRate(rs.getDouble("AvgRoomRate"));
        data.setClassifiedSeason(rs.getString("ClassifiedSeason"));
        data.setImportDate(rs.getTimestamp("ImportDate"));
        return data;
    }

    private static SeasonThreshold mapResultSetToThreshold(ResultSet rs) throws SQLException {
        SeasonThreshold threshold = new SeasonThreshold();
        threshold.setThresholdId(rs.getInt("ThresholdID"));
        threshold.setUserId(rs.getInt("UserID"));
        threshold.setThresholdLowNormal(rs.getDouble("ThresholdLowNormal"));
        threshold.setThresholdNormalPeak(rs.getDouble("ThresholdNormalPeak"));
        threshold.setThresholdPeakSuperPeak(rs.getDouble("ThresholdPeakSuperPeak"));
        threshold.setAutoGenerated(rs.getBoolean("IsAutoGenerated"));
        threshold.setCreatedDate(rs.getTimestamp("CreatedDate"));
        threshold.setLastUpdated(rs.getTimestamp("LastUpdated"));
        return threshold;
    }
}
