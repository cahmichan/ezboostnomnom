package com.ezboost.dao;

import com.ezboost.model.MarketSegment;
import com.ezboost.util.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * MarketSegmentDAO - Data Access Object for market segments
 *
 * Each user has their own set of market segments (per-user isolation).
 * Revenue share column is ignored (kept for backward compatibility but not used).
 */
public class MarketSegmentDAO {

    private static final Logger logger = LoggerFactory.getLogger(MarketSegmentDAO.class);

    /**
     * Initialize default market segments for a specific user.
     * Only creates defaults if the user has no segments yet.
     */
    public static void initializeDefaultSegments(int userId) {
        // Check if this user already has segments
        if (!getAllSegments(userId).isEmpty()) {
            return;
        }

        MarketSegment[] defaults = {
            // FIT Segments (Free Independent Travelers)
            new MarketSegment("Travel Agent", "TA", "FIT", 0.90,
                "Negotiated wholesale rates for travel agents"),
            new MarketSegment("OTA", "OTA", "FIT", 1.20,
                "Online Travel Agencies - commission passed to customer"),
            new MarketSegment("Website Direct", "WEB", "FIT", 1.00,
                "Direct bookings via hotel website"),
            new MarketSegment("Walk-in", "WALK", "FIT", 1.15,
                "Walk-in guests - premium convenience pricing"),
            new MarketSegment("Long Stay", "LONG", "FIT", 0.75,
                "Extended stay discounts (weekly/monthly)"),

            // GIT Segments (Group Inclusive Tours)
            new MarketSegment("Corporate Group", "CORP", "GIT", 1.05,
                "Corporate events and meetings"),
            new MarketSegment("Government", "GOV", "GIT", 0.85,
                "Government contracted rates"),
            new MarketSegment("Tour Group", "TOUR", "GIT", 0.80,
                "Tour operator group rates")
        };

        for (MarketSegment segment : defaults) {
            segment.setUserId(userId);
            saveSegment(segment);
        }
    }

    /**
     * Get all active market segments for a specific user
     */
    public static List<MarketSegment> getAllSegments(int userId) {
        List<MarketSegment> segments = new ArrayList<>();
        String sql = "SELECT id, user_id, segment_name, segment_code, category, rate_multiplier, description, active " +
                     "FROM MarketSegment WHERE active = true AND user_id = ? ORDER BY category, segment_name";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                segments.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching all segments", e);
        }

        return segments;
    }

    /**
     * Get segment by code for a specific user
     */
    public static MarketSegment getSegmentByCode(String code, int userId) {
        String sql = "SELECT id, user_id, segment_name, segment_code, category, rate_multiplier, description, active " +
                     "FROM MarketSegment WHERE segment_code = ? AND user_id = ? AND active = true";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, code);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSet(rs);
            }
        } catch (SQLException e) {
            logger.error("Error fetching segment by code", e);
        }

        return null;
    }

    /**
     * Save or update a market segment (scoped to user via segment.getUserId())
     */
    public static boolean saveSegment(MarketSegment segment) {
        // Check if exists for this user
        MarketSegment existing = getSegmentByCodeIncludingInactive(segment.getSegmentCode(), segment.getUserId());

        if (existing != null) {
            // Update
            String sql = "UPDATE MarketSegment SET segment_name = ?, category = ?, " +
                        "rate_multiplier = ?, description = ?, active = true WHERE segment_code = ? AND user_id = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, segment.getSegmentName());
                stmt.setString(2, segment.getCategory());
                stmt.setDouble(3, segment.getRateMultiplier());
                stmt.setString(4, segment.getDescription());
                stmt.setString(5, segment.getSegmentCode());
                stmt.setInt(6, segment.getUserId());
                return stmt.executeUpdate() > 0;
            } catch (SQLException e) {
                logger.error("Error updating segment", e);
            }
        } else {
            // Insert
            String sql = "INSERT INTO MarketSegment (segment_name, segment_code, category, " +
                        "rate_multiplier, revenue_share, description, active, user_id) VALUES (?, ?, ?, ?, 0.0, ?, true, ?)";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, segment.getSegmentName());
                stmt.setString(2, segment.getSegmentCode());
                stmt.setString(3, segment.getCategory());
                stmt.setDouble(4, segment.getRateMultiplier());
                stmt.setString(5, segment.getDescription());
                stmt.setInt(6, segment.getUserId());
                return stmt.executeUpdate() > 0;
            } catch (SQLException e) {
                logger.error("Error inserting segment", e);
            }
        }
        return false;
    }

    /**
     * Soft delete a segment for a specific user (set active = false)
     */
    public static void deleteSegment(String code, int userId) {
        String sql = "UPDATE MarketSegment SET active = false WHERE segment_code = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, code);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting segment", e);
        }
    }

    /**
     * Hard delete all segments for a specific user (for reset)
     */
    public static void deleteAllSegments(int userId) {
        String sql = "DELETE FROM MarketSegment WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting all segments", e);
        }
    }

    private static MarketSegment getSegmentByCodeIncludingInactive(String code, int userId) {
        String sql = "SELECT id, user_id, segment_name, segment_code, category, rate_multiplier, description, active " +
                "FROM MarketSegment WHERE segment_code = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, code);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSet(rs);
            }
        } catch (SQLException e) {
            logger.error("Error fetching segment by code including inactive", e);
        }

        return null;
    }

    /**
     * Helper to map a ResultSet row to a MarketSegment object
     */
    private static MarketSegment mapResultSet(ResultSet rs) throws SQLException {
        MarketSegment segment = new MarketSegment();
        segment.setId(rs.getInt("id"));
        segment.setUserId(rs.getInt("user_id"));
        segment.setSegmentName(rs.getString("segment_name"));
        segment.setSegmentCode(rs.getString("segment_code"));
        segment.setCategory(rs.getString("category"));
        segment.setRateMultiplier(rs.getDouble("rate_multiplier"));
        segment.setDescription(rs.getString("description"));
        segment.setActive(rs.getBoolean("active"));
        return segment;
    }
}
