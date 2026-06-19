package com.ezboost.dao;

import com.ezboost.util.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;

/** Stores the information required to explain and reproduce an optimization run. */
public final class OptimizationRunMetadataDAO {

    private static final Logger logger = LoggerFactory.getLogger(OptimizationRunMetadataDAO.class);

    private OptimizationRunMetadataDAO() {
    }

    public static void save(int requestId, int userId, double targetRevenue, double achievedRevenue,
                            long randomSeed, String demandCurveMode) {
        String sql = "INSERT INTO OptimizationRunMetadata " +
                "(request_id, user_id, target_revenue, achieved_revenue, random_seed, algorithm_version, demand_curve_mode, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, requestId);
            stmt.setInt(2, userId);
            stmt.setDouble(3, targetRevenue);
            stmt.setDouble(4, achievedRevenue);
            stmt.setLong(5, randomSeed);
            stmt.setString(6, "seasonal-ga-v1");
            stmt.setString(7, demandCurveMode);
            stmt.executeUpdate();
        } catch (Exception e) {
            logger.warn("Could not save optimization metadata for request {}", requestId, e);
        }
    }
}
