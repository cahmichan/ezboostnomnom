package com.ezboost.dao;

import com.ezboost.util.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/** Stores a self-contained, owner-scoped export snapshot for completed runs. */
public final class OptimizationReportSnapshotDAO {

    private static final Logger logger = LoggerFactory.getLogger(OptimizationReportSnapshotDAO.class);

    private OptimizationReportSnapshotDAO() {
    }

    public static void save(int requestId, int userId, String payload) {
        String delete = "DELETE FROM OptimizationReportSnapshot WHERE request_id = ? AND user_id = ?";
        String insert = "INSERT INTO OptimizationReportSnapshot (request_id, user_id, payload, created_at) " +
                "VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement deleteStmt = conn.prepareStatement(delete);
                 PreparedStatement insertStmt = conn.prepareStatement(insert)) {
                deleteStmt.setInt(1, requestId);
                deleteStmt.setInt(2, userId);
                deleteStmt.executeUpdate();
                insertStmt.setInt(1, requestId);
                insertStmt.setInt(2, userId);
                insertStmt.setString(3, payload);
                insertStmt.executeUpdate();
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            logger.warn("Could not save report snapshot for request {}", requestId, e);
        }
    }

    public static String load(int requestId, int userId) {
        String sql = "SELECT payload FROM OptimizationReportSnapshot WHERE request_id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, requestId);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("payload") : null;
            }
        } catch (Exception e) {
            logger.warn("Could not load report snapshot {} for user {}", requestId, userId, e);
            return null;
        }
    }
}
