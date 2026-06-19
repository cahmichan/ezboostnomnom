package com.ezboost.dao;

import com.ezboost.model.Room;
import com.ezboost.model.Season;
import com.ezboost.util.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class OptimizationResultDAO {

    private static final Logger logger = LoggerFactory.getLogger(OptimizationResultDAO.class);

    public static void saveResult(int requestId, List<Room> rooms, double totalEstimatedRevenue) {
        String sql = "INSERT INTO OptimizationResult (RequestID, RoomType, NumberofRoom, ExpectedOccupancyRate, " +
                "LowSeasonPrice, NormalSeasonPrice, PeakSeasonPrice, SuperPeakSeasonPrice, EstimatedProfit, TotalEstimatedProfit) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (Room room : rooms) {
                pstmt.setInt(1, requestId);
                pstmt.setString(2, room.getName());
                pstmt.setInt(3, room.getTotalRooms());

                double savedOccupancy;
                if (room.hasSeasonalOccupancies()) {
                    double sum = 0;
                    for (Season s : Season.values()) {
                        sum += room.getOccupancyForSeason(s);
                    }
                    savedOccupancy = sum / Season.values().length;
                } else {
                    savedOccupancy = room.getOccupancy();
                }
                pstmt.setDouble(4, savedOccupancy);

                pstmt.setDouble(5, room.getSeasonalPrices().get(Season.LOW));
                pstmt.setDouble(6, room.getSeasonalPrices().get(Season.NORMAL));
                pstmt.setDouble(7, room.getSeasonalPrices().get(Season.PEAK));
                pstmt.setDouble(8, room.getSeasonalPrices().get(Season.SUPER_PEAK));
                pstmt.setDouble(9, room.getEstimatedRevenue());
                pstmt.setDouble(10, totalEstimatedRevenue);

                pstmt.executeUpdate();
            }

        } catch (Exception e) {
            logger.error("Failed to save optimization result for request {}", requestId, e);
            throw new RuntimeException("Error saving optimization results", e);
        }
    }

    public static double getBestTotalRevenue(int userId) {
        String sql = "SELECT MAX(r.TotalEstimatedProfit) FROM OptimizationResult r " +
                "JOIN OptimizationRequest req ON r.RequestID = req.RequestID " +
                "WHERE req.UserID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load best revenue for user {}", userId, e);
        }
        return 0.0;
    }

    public static double getAverageTotalRevenue(int userId) {
        String sql = "SELECT AVG(sub.MaxRevenue) FROM (" +
                "SELECT MAX(r.TotalEstimatedProfit) AS MaxRevenue " +
                "FROM OptimizationResult r " +
                "JOIN OptimizationRequest req ON r.RequestID = req.RequestID " +
                "WHERE req.UserID = ? " +
                "GROUP BY r.RequestID) sub";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load average revenue for user {}", userId, e);
        }
        return 0.0;
    }

    public static double getBestTotalProfit(int userId) {
        return getBestTotalRevenue(userId);
    }

    public static double getAverageTotalProfit(int userId) {
        return getAverageTotalRevenue(userId);
    }
}
