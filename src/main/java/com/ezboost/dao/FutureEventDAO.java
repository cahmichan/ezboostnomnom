package com.ezboost.dao;

import com.ezboost.model.FutureEvent;
import com.ezboost.util.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FutureEventDAO {

    private static final Logger logger = LoggerFactory.getLogger(FutureEventDAO.class);

    /**
     * Schema creation now happens at application startup.
     * Kept for compatibility with older call sites.
     */
    public static void initializeTables() {
        logger.debug("FutureEventDAO.initializeTables() called; startup migration already handled schema creation.");
    }

    /**
     * Get all active events for a user
     */
    public static List<FutureEvent> getAllEvents(int userId) {
        List<FutureEvent> events = new ArrayList<>();
        String sql = "SELECT * FROM FutureEvent WHERE user_id = ? AND active = TRUE ORDER BY event_date";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                events.add(mapResultSet(rs));
            }
            logger.debug("Loaded {} events for userId: {}", events.size(), userId);
        } catch (SQLException e) {
            logger.error("Error fetching events", e);
        }
        return events;
    }

    /**
     * Get all events (including inactive) for a user
     */
    public static List<FutureEvent> getAllEventsIncludingInactive(int userId) {
        List<FutureEvent> events = new ArrayList<>();
        String sql = "SELECT * FROM FutureEvent WHERE user_id = ? ORDER BY event_date";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                events.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching all events", e);
        }
        return events;
    }

    /**
     * Get events by year
     */
    public static List<FutureEvent> getEventsByYear(int userId, int year) {
        List<FutureEvent> events = new ArrayList<>();
        String sql = "SELECT * FROM FutureEvent WHERE user_id = ? AND active = TRUE " +
                     "AND (YEAR(event_date) = ? OR (event_end_date IS NOT NULL AND YEAR(event_end_date) = ?)) " +
                     "ORDER BY event_date";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, year);
            pstmt.setInt(3, year);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                events.add(mapResultSet(rs));
            }
            logger.debug("Loaded {} events for year {}", events.size(), year);
        } catch (SQLException e) {
            logger.error("Error fetching events by year", e);
        }
        return events;
    }

    /**
     * Get events by month (events that overlap with the given month)
     */
    public static List<FutureEvent> getEventsByMonth(int userId, int year, int month) {
        List<FutureEvent> events = new ArrayList<>();
        // Build first and last day of month
        String monthStart = String.format("%d-%02d-01", year, month);
        // Last day: use next month's first day minus 1
        String monthEnd;
        if (month == 12) {
            monthEnd = String.format("%d-12-31", year);
        } else {
            monthEnd = String.format("%d-%02d-01", year, month + 1);
        }

        String sql = "SELECT * FROM FutureEvent WHERE user_id = ? AND active = TRUE " +
                     "AND event_date < ? " +
                     "AND (event_end_date IS NULL AND YEAR(event_date) = ? AND MONTH(event_date) = ? " +
                     " OR event_end_date IS NOT NULL AND event_end_date >= ?) " +
                     "ORDER BY event_date";

        // Simpler approach: just check if event_date is in the month, OR if the event range overlaps
        String simpleSql = "SELECT * FROM FutureEvent WHERE user_id = ? AND active = TRUE " +
                "AND (" +
                "  (event_end_date IS NULL AND YEAR(event_date) = ? AND MONTH(event_date) = ?)" +
                "  OR " +
                "  (event_end_date IS NOT NULL AND event_date <= ? AND event_end_date >= ?)" +
                ") ORDER BY event_date";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(simpleSql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, year);
            pstmt.setInt(3, month);
            pstmt.setDate(4, Date.valueOf(monthEnd.equals(String.format("%d-12-31", year)) ? monthEnd :
                    String.format("%d-%02d-%02d", year, month, getLastDayOfMonth(year, month))));
            pstmt.setDate(5, Date.valueOf(monthStart));

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                events.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching events by month", e);
        }
        return events;
    }

    /**
     * Save a new event
     */
    public static void saveEvent(FutureEvent event) {
        String sql = "INSERT INTO FutureEvent (user_id, event_name, event_date, event_end_date, " +
                     "event_type, season_override, source, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, event.getUserId());
            pstmt.setString(2, event.getEventName());
            pstmt.setDate(3, event.getEventDate());
            if (event.getEventEndDate() != null) {
                pstmt.setDate(4, event.getEventEndDate());
            } else {
                pstmt.setNull(4, Types.DATE);
            }
            pstmt.setString(5, event.getEventType());
            pstmt.setString(6, event.getSeasonOverride());
            pstmt.setString(7, event.getSource());
            pstmt.setBoolean(8, event.isActive());

            pstmt.executeUpdate();
            logger.debug("Saved event: {}", event.getEventName());
        } catch (SQLException e) {
            logger.error("Error saving event", e);
        }
    }

    /**
     * Update an existing event
     */
    public static void updateEvent(FutureEvent event) {
        String sql = "UPDATE FutureEvent SET event_name = ?, event_date = ?, event_end_date = ?, " +
                     "event_type = ?, season_override = ?, active = ? " +
                     "WHERE event_id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, event.getEventName());
            pstmt.setDate(2, event.getEventDate());
            if (event.getEventEndDate() != null) {
                pstmt.setDate(3, event.getEventEndDate());
            } else {
                pstmt.setNull(3, Types.DATE);
            }
            pstmt.setString(4, event.getEventType());
            pstmt.setString(5, event.getSeasonOverride());
            pstmt.setBoolean(6, event.isActive());
            pstmt.setInt(7, event.getEventId());
            pstmt.setInt(8, event.getUserId());

            int rows = pstmt.executeUpdate();
            logger.debug("Updated event ID {} ({} rows)", event.getEventId(), rows);
        } catch (SQLException e) {
            logger.error("Error updating event", e);
        }
    }

    /**
     * Delete a single event
     */
    public static void deleteEvent(int eventId, int userId) {
        String sql = "DELETE FROM FutureEvent WHERE event_id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, eventId);
            pstmt.setInt(2, userId);
            int rows = pstmt.executeUpdate();
            logger.debug("Deleted event ID {} ({} rows)", eventId, rows);
        } catch (SQLException e) {
            logger.error("Error deleting event", e);
        }
    }

    /**
     * Delete all events for a user
     */
    public static void deleteAllEvents(int userId) {
        String sql = "DELETE FROM FutureEvent WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            int rows = pstmt.executeUpdate();
            logger.debug("Deleted all events for user {} ({} rows)", userId, rows);
        } catch (SQLException e) {
            logger.error("Error deleting all events", e);
        }
    }

    /**
     * Delete events by source (e.g., delete all CALENDARIFIC or PRESET events)
     */
    public static void deleteEventsBySource(int userId, String source) {
        String sql = "DELETE FROM FutureEvent WHERE user_id = ? AND source = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setString(2, source);
            int rows = pstmt.executeUpdate();
            logger.debug("Deleted {} events for user {} ({} rows)", source, userId, rows);
        } catch (SQLException e) {
            logger.error("Error deleting events by source", e);
        }
    }

    /**
     * Delete events by source and year
     */
    public static void deleteEventsBySourceAndYear(int userId, String source, int year) {
        String sql = "DELETE FROM FutureEvent WHERE user_id = ? AND source = ? AND YEAR(event_date) = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setString(2, source);
            pstmt.setInt(3, year);
            int rows = pstmt.executeUpdate();
            logger.debug("Deleted {} events for year {} ({} rows)", source, year, rows);
        } catch (SQLException e) {
            logger.error("Error deleting events by source/year", e);
        }
    }

    /**
     * Batch save events (for API fetch and preset loading)
     */
    public static void batchSaveEvents(List<FutureEvent> events) {
        if (events == null || events.isEmpty()) return;

        String sql = "INSERT INTO FutureEvent (user_id, event_name, event_date, event_end_date, " +
                     "event_type, season_override, source, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (FutureEvent event : events) {
                    pstmt.setInt(1, event.getUserId());
                    pstmt.setString(2, event.getEventName());
                    pstmt.setDate(3, event.getEventDate());
                    if (event.getEventEndDate() != null) {
                        pstmt.setDate(4, event.getEventEndDate());
                    } else {
                        pstmt.setNull(4, Types.DATE);
                    }
                    pstmt.setString(5, event.getEventType());
                    pstmt.setString(6, event.getSeasonOverride());
                    pstmt.setString(7, event.getSource());
                    pstmt.setBoolean(8, event.isActive());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
            conn.commit();
            logger.debug("Batch saved {} events", events.size());
        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            logger.error("Error batch saving events", e);
        } finally {
            if (conn != null) { try { conn.close(); } catch (SQLException ignored) {} }
        }
    }

    // ==================== API KEY OPERATIONS ====================

    /**
     * Get Calendarific API key for user
     */
    public static String getApiKey(int userId) {
        String sql = "SELECT setting_value FROM UserApiSettings WHERE user_id = ? AND setting_key = 'calendarific_api_key'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("setting_value");
            }
        } catch (SQLException e) {
            logger.error("Error fetching API key", e);
        }
        return null;
    }

    /**
     * Save/update Calendarific API key for user
     */
    public static void saveApiKey(int userId, String apiKey) {
        // Try update first
        String updateSql = "UPDATE UserApiSettings SET setting_value = ? WHERE user_id = ? AND setting_key = 'calendarific_api_key'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateSql)) {

            pstmt.setString(1, apiKey);
            pstmt.setInt(2, userId);
            int rows = pstmt.executeUpdate();

            if (rows == 0) {
                // Insert new
                String insertSql = "INSERT INTO UserApiSettings (user_id, setting_key, setting_value) VALUES (?, 'calendarific_api_key', ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setInt(1, userId);
                    insertStmt.setString(2, apiKey);
                    insertStmt.executeUpdate();
                }
            }
            logger.debug("Saved API key for user {}", userId);
        } catch (SQLException e) {
            logger.error("Error saving API key", e);
        }
    }

    // ==================== HELPER METHODS ====================

    private static FutureEvent mapResultSet(ResultSet rs) throws SQLException {
        FutureEvent event = new FutureEvent();
        event.setEventId(rs.getInt("event_id"));
        event.setUserId(rs.getInt("user_id"));
        event.setEventName(rs.getString("event_name"));
        event.setEventDate(rs.getDate("event_date"));
        event.setEventEndDate(rs.getDate("event_end_date"));
        event.setEventType(rs.getString("event_type"));
        event.setSeasonOverride(rs.getString("season_override"));
        event.setSource(rs.getString("source"));
        event.setActive(rs.getBoolean("active"));
        return event;
    }

    private static int getLastDayOfMonth(int year, int month) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(year, month - 1, 1);
        return cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
    }
}
