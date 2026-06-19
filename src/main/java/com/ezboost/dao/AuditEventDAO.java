package com.ezboost.dao;

import com.ezboost.util.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;

/** Records user-visible mutations without storing request payloads or secrets. */
public final class AuditEventDAO {

    private static final Logger logger = LoggerFactory.getLogger(AuditEventDAO.class);

    private AuditEventDAO() {
    }

    public static void record(int userId, String action, String entityType, String outcome) {
        String sql = "INSERT INTO AuditEvent (user_id, action, entity_type, outcome, created_at) " +
                "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, action);
            stmt.setString(3, entityType);
            stmt.setString(4, outcome);
            stmt.executeUpdate();
        } catch (Exception e) {
            // Audit failure must not make an otherwise valid user operation fail.
            logger.warn("Could not write audit event {} for user {}", action, userId, e);
        }
    }
}
