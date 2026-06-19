package com.ezboost.dao;

import com.ezboost.util.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OptimizationRequestDAO {

    private static final Logger logger = LoggerFactory.getLogger(OptimizationRequestDAO.class);

    public static int createRequest(int userId) {
        String sql = "INSERT INTO OptimizationRequest (UserID, RequestDate) VALUES (?, CURRENT_TIMESTAMP)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, userId);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new RuntimeException("Creating optimization request failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
                throw new RuntimeException("Creating optimization request failed, no ID obtained.");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error creating optimization request", e);
        }
    }

    public static int getOptimizationCount(int userId) {
        String sql = "SELECT COUNT(*) FROM OptimizationRequest WHERE UserID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to count optimizations for user {}", userId, e);
        }
        return 0;
    }

    public static Timestamp getLastOptimizationDate(int userId) {
        String sql = "SELECT MAX(RequestDate) FROM OptimizationRequest WHERE UserID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getTimestamp(1);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load last optimization date for user {}", userId, e);
        }
        return null;
    }

    public static List<Map<String, Object>> getRecentOptimizations(int userId, int limit) {
        List<Map<String, Object>> results = new ArrayList<>();
        String sql = "SELECT req.RequestID, req.RequestDate, " +
                "MAX(res.TotalEstimatedProfit) AS TotalRevenue " +
                "FROM OptimizationRequest req " +
                "LEFT JOIN OptimizationResult res ON req.RequestID = res.RequestID " +
                "WHERE req.UserID = ? " +
                "GROUP BY req.RequestID, req.RequestDate " +
                "ORDER BY req.RequestDate DESC " +
                "FETCH FIRST " + limit + " ROWS ONLY";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("requestId", rs.getInt("RequestID"));
                    row.put("date", rs.getTimestamp("RequestDate"));
                    row.put("totalRevenue", rs.getDouble("TotalRevenue"));
                    results.add(row);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load recent optimizations for user {}", userId, e);
        }
        return results;
    }
}
