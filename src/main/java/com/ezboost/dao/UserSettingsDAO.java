package com.ezboost.dao;

import com.ezboost.model.UserMultiplierSettings;
import com.ezboost.util.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * UserSettingsDAO - Database operations for user multiplier settings
 */
public class UserSettingsDAO {

    private static final Logger logger = LoggerFactory.getLogger(UserSettingsDAO.class);

    /**
     * Save or update a multiplier setting (alias for saveMultiplierSetting)
     */
    public static int saveOrUpdateSetting(UserMultiplierSettings setting) {
        return saveMultiplierSetting(setting);
    }

    /**
     * Save or update a multiplier setting
     */
    public static int saveMultiplierSetting(UserMultiplierSettings setting) {
        // Check if setting already exists
        UserMultiplierSettings existing = getSettingByContext(
            setting.getUserId(), 
            setting.getRoomType(), 
            setting.getSeasonName(), 
            setting.getSegmentName()
        );

        if (existing != null) {
            // Update existing setting
            setting.setSettingId(existing.getSettingId());
            updateMultiplierSetting(setting);
            return existing.getSettingId();
        } else {
            // Insert new setting
            return insertMultiplierSetting(setting);
        }
    }

    /**
     * Insert a new multiplier setting
     */
    private static int insertMultiplierSetting(UserMultiplierSettings setting) {
        String sql = "INSERT INTO UserMultiplierSettings (UserID, RoomType, SeasonName, " +
                     "SegmentName, CustomMultiplier, MinBound, MaxBound, IsLocked) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, setting.getUserId());
            
            if (setting.getRoomType() != null) {
                pstmt.setString(2, setting.getRoomType());
            } else {
                pstmt.setNull(2, Types.VARCHAR);
            }
            
            pstmt.setString(3, setting.getSeasonName());
            
            if (setting.getSegmentName() != null) {
                pstmt.setString(4, setting.getSegmentName());
            } else {
                pstmt.setNull(4, Types.VARCHAR);
            }
            
            pstmt.setDouble(5, setting.getCustomMultiplier());
            pstmt.setDouble(6, setting.getMinBound());
            pstmt.setDouble(7, setting.getMaxBound());
            pstmt.setBoolean(8, setting.isLocked());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    logger.debug("Inserted setting ID: {} for season: {}", id, setting.getSeasonName());
                    return id;
                }
            }
        } catch (SQLException e) {
            logger.error("Error inserting setting", e);
        }
        return -1;
    }

    /**
     * Update an existing multiplier setting
     */
    public static boolean updateMultiplierSetting(UserMultiplierSettings setting) {
        String sql = "UPDATE UserMultiplierSettings SET CustomMultiplier=?, MinBound=?, " +
                     "MaxBound=?, IsLocked=?, LastUpdated=CURRENT_TIMESTAMP WHERE SettingID=? AND UserID=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, setting.getCustomMultiplier());
            pstmt.setDouble(2, setting.getMinBound());
            pstmt.setDouble(3, setting.getMaxBound());
            pstmt.setBoolean(4, setting.isLocked());
            pstmt.setInt(5, setting.getSettingId());
            pstmt.setInt(6, setting.getUserId());

            boolean success = pstmt.executeUpdate() > 0;
            if (success) {
                logger.debug("Updated setting ID: {} to multiplier: {}", setting.getSettingId(), setting.getCustomMultiplier());
            }
            return success;
        } catch (SQLException e) {
            logger.error("Error updating setting", e);
        }
        return false;
    }

    /**
     * Get all settings for a user
     */
    public static List<UserMultiplierSettings> getUserSettings(int userId) {
        List<UserMultiplierSettings> settings = new ArrayList<>();
        String sql = "SELECT * FROM UserMultiplierSettings WHERE UserID = ? ORDER BY SeasonName, RoomType";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                settings.add(mapResultSetToSetting(rs));
            }
            logger.debug("Loaded {} settings for userId: {}", settings.size(), userId);
        } catch (SQLException e) {
            logger.error("Error fetching user settings", e);
        }
        return settings;
    }

    /**
     * Get specific setting by context (room, season, segment)
     */
    public static UserMultiplierSettings getSettingByContext(int userId, String roomType, 
                                                              String seasonName, String segmentName) {
        StringBuilder sql = new StringBuilder(
            "SELECT * FROM UserMultiplierSettings WHERE UserID = ? AND SeasonName = ?"
        );

        if (roomType != null) {
            sql.append(" AND RoomType = ?");
        } else {
            sql.append(" AND RoomType IS NULL");
        }

        if (segmentName != null) {
            sql.append(" AND SegmentName = ?");
        } else {
            sql.append(" AND SegmentName IS NULL");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            pstmt.setInt(paramIndex++, userId);
            pstmt.setString(paramIndex++, seasonName);
            if (roomType != null) pstmt.setString(paramIndex++, roomType);
            if (segmentName != null) pstmt.setString(paramIndex++, segmentName);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToSetting(rs);
            }
        } catch (SQLException e) {
            logger.error("Error fetching setting by context", e);
        }
        return null;
    }

    /**
     * Get the applicable multiplier for a given context
     */
    public static double getApplicableMultiplier(int userId, String roomType, 
                                                  String seasonName, String segmentName) {
        List<UserMultiplierSettings> allSettings = getUserSettings(userId);
        UserMultiplierSettings bestMatch = null;
        int highestSpecificity = -1;

        for (UserMultiplierSettings setting : allSettings) {
            if (setting.appliesTo(roomType, seasonName, segmentName)) {
                int specificity = setting.getSpecificityScore();
                if (specificity > highestSpecificity) {
                    highestSpecificity = specificity;
                    bestMatch = setting;
                }
            }
        }

        if (bestMatch != null) {
            return bestMatch.getCustomMultiplier();
        }

        // Return default multiplier based on season
        switch (seasonName) {
            case "SUPER_PEAK": return 1.35;
            case "PEAK": return 1.15;
            case "NORMAL": return 1.0;
            case "LOW": return 0.85;
            default: return 1.0;
        }
    }

    /**
     * Lock/unlock a setting
     */
    public static boolean toggleLock(int settingId, int userId, boolean locked) {
        String sql = "UPDATE UserMultiplierSettings SET IsLocked = ?, LastUpdated = CURRENT_TIMESTAMP " +
                     "WHERE SettingID = ? AND UserID = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBoolean(1, locked);
            pstmt.setInt(2, settingId);
            pstmt.setInt(3, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error toggling lock", e);
        }
        return false;
    }

    /**
     * Set lock status (alias for toggleLock)
     */
    public static boolean setLockStatus(int settingId, int userId, boolean locked) {
        return toggleLock(settingId, userId, locked);
    }

    /**
     * Delete a multiplier setting
     */
    public static boolean deleteSetting(int settingId, int userId) {
        String sql = "DELETE FROM UserMultiplierSettings WHERE SettingID = ? AND UserID = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, settingId);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting setting", e);
        }
        return false;
    }

    /**
     * Delete all settings for a user
     */
    public static boolean deleteAllUserSettings(int userId) {
        String sql = "DELETE FROM UserMultiplierSettings WHERE UserID = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            int deleted = pstmt.executeUpdate();
            logger.debug("Deleted {} settings for userId: {}", deleted, userId);
            return true;
        } catch (SQLException e) {
            logger.error("Error deleting user settings", e);
        }
        return false;
    }

    /**
     * Initialize default settings for a user (all seasons, no room/segment specificity)
     */
    public static void initializeDefaultSettings(int userId) {
        String[] seasons = {"LOW", "NORMAL", "PEAK", "SUPER_PEAK"};
        double[] defaults = {0.85, 1.0, 1.15, 1.35};

        for (int i = 0; i < seasons.length; i++) {
            UserMultiplierSettings setting = new UserMultiplierSettings();
            setting.setUserId(userId);
            setting.setRoomType(null);  // Global - applies to all rooms
            setting.setSeasonName(seasons[i]);
            setting.setSegmentName(null);  // Global - applies to all segments
            setting.setCustomMultiplier(defaults[i]);
            setting.setMinBound(0.5);
            setting.setMaxBound(2.0);
            setting.setLocked(false);
            
            saveMultiplierSetting(setting);
        }
        logger.debug("Initialized default settings for userId: {}", userId);
    }

    /**
     * Helper method to map ResultSet to UserMultiplierSettings
     */
    private static UserMultiplierSettings mapResultSetToSetting(ResultSet rs) throws SQLException {
        UserMultiplierSettings setting = new UserMultiplierSettings();
        setting.setSettingId(rs.getInt("SettingID"));
        setting.setUserId(rs.getInt("UserID"));
        setting.setRoomType(rs.getString("RoomType"));
        setting.setSeasonName(rs.getString("SeasonName"));
        setting.setSegmentName(rs.getString("SegmentName"));
        setting.setCustomMultiplier(rs.getDouble("CustomMultiplier"));
        setting.setMinBound(rs.getDouble("MinBound"));
        setting.setMaxBound(rs.getDouble("MaxBound"));
        setting.setLocked(rs.getBoolean("IsLocked"));
        setting.setLastUpdated(rs.getTimestamp("LastUpdated"));
        return setting;
    }
}
