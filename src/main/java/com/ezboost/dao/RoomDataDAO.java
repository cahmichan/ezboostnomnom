package com.ezboost.dao;

import com.ezboost.model.Room;
import com.ezboost.util.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RoomDataDAO {

    private static final Logger logger = LoggerFactory.getLogger(RoomDataDAO.class);
    private static final String ROOM_SELECT_SQL =
            "SELECT RoomType, COALESCE(BaseADR, MinADR) AS BaseADR, MinADR, MaxADR, Occupancy, NumberOfRoom " +
            "FROM ActualRoomData WHERE UserID = ?";

    public static List<Room> getAllRooms(int userId) {
        List<Room> rooms = new ArrayList<>();
        String sql = ROOM_SELECT_SQL + " ORDER BY RoomType";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    rooms.add(mapRoom(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching rooms for user {}", userId, e);
        }

        return rooms;
    }

    public static int saveRoomData(int userId, List<Room> rooms, boolean replaceExisting) {
        String deleteSql = "DELETE FROM ActualRoomData WHERE UserID = ?";
        String insertSql = "INSERT INTO ActualRoomData " +
                "(UserID, RoomType, BaseADR, BaseADRWasBackfilled, MinADR, MaxADR, Occupancy, NumberOfRoom) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        int successCount = 0;

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            if (replaceExisting) {
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                    deleteStmt.setInt(1, userId);
                    deleteStmt.executeUpdate();
                }
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                for (Room room : rooms) {
                    insertStmt.setInt(1, userId);
                    insertStmt.setString(2, room.getName());
                    insertStmt.setDouble(3, room.getBaseAdr());
                    insertStmt.setBoolean(4, false);
                    insertStmt.setDouble(5, room.getMinAdr());
                    insertStmt.setDouble(6, room.getMaxAdr());
                    insertStmt.setDouble(7, room.getOccupancy());
                    insertStmt.setInt(8, room.getTotalRooms());
                    insertStmt.addBatch();
                }

                int[] results = insertStmt.executeBatch();
                for (int result : results) {
                    if (result > 0 || result == Statement.SUCCESS_NO_INFO) {
                        successCount++;
                    }
                }
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackError) {
                    logger.warn("Rollback failed while saving room data for user {}", userId, rollbackError);
                }
            }
            logger.error("Error saving room data for user {}", userId, e);
            throw new RuntimeException("Unable to save room data", e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException closeError) {
                    logger.warn("Failed to close room data connection for user {}", userId, closeError);
                }
            }
        }

        return successCount;
    }

    public static boolean deleteAllRoomData(int userId) {
        String sql = "DELETE FROM ActualRoomData WHERE UserID = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            int deleted = pstmt.executeUpdate();
            logger.debug("Deleted {} room records for user {}", deleted, userId);
            return true;
        } catch (SQLException e) {
            logger.error("Error clearing rooms for user {}", userId, e);
        }

        return false;
    }

    public static int getTotalRoomCount(int userId) {
        String sql = "SELECT COALESCE(SUM(NumberOfRoom), 0) AS Total FROM ActualRoomData WHERE UserID = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("Total");
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting total room count for user {}", userId, e);
        }

        return 0;
    }

    public static int getRoomTypeCount(int userId) {
        String sql = "SELECT COUNT(*) AS Count FROM ActualRoomData WHERE UserID = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("Count");
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting room type count for user {}", userId, e);
        }

        return 0;
    }

    public static boolean hasRoomData(int userId) {
        return getRoomTypeCount(userId) > 0;
    }

    public static double getWeightedAverageBaseRate(int userId) {
        List<Room> rooms = getAllRooms(userId);
        double totalWeightedRate = 0;
        int totalRooms = 0;

        for (Room room : rooms) {
            totalWeightedRate += room.getBaseAdr() * room.getTotalRooms();
            totalRooms += room.getTotalRooms();
        }

        return totalRooms > 0 ? totalWeightedRate / totalRooms : 0;
    }

    private static Room mapRoom(ResultSet rs) throws SQLException {
        return new Room(
                rs.getString("RoomType"),
                rs.getDouble("BaseADR"),
                rs.getDouble("MinADR"),
                rs.getDouble("MaxADR"),
                rs.getDouble("Occupancy"),
                rs.getInt("NumberOfRoom")
        );
    }
}
