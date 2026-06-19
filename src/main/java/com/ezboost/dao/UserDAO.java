package com.ezboost.dao;

import com.ezboost.model.User;
import com.ezboost.util.DBConnection;
import com.ezboost.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    private static final Logger logger = LoggerFactory.getLogger(UserDAO.class);

    public static boolean registerUser(User user) {
        String sql = "INSERT INTO \"USER\" (FIRSTNAME, LASTNAME, USERNAME, EMAIL, PASSWORD, PHONENUMBER, " +
                "ONBOARDING_REQUIRED, ONBOARDING_COMPLETED, ONBOARDING_STEP, EMAIL_KEY, USERNAME_KEY) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getFirstName());
            stmt.setString(2, user.getLastName());
            stmt.setString(3, user.getUsername());
            stmt.setString(4, user.getEmail());
            stmt.setString(5, PasswordUtil.ensureHashed(user.getPassword()));
            stmt.setString(6, user.getPhoneNumber());
            stmt.setBoolean(7, true);
            stmt.setBoolean(8, false);
            stmt.setString(9, "IMPORT");
            stmt.setString(10, canonical(user.getEmail()));
            stmt.setString(11, canonical(user.getUsername()));

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to register user {}", user.getEmail(), e);
        }
        return false;
    }

    public static boolean isUsernameTaken(String username) {
        return isUsernameTaken(username, null);
    }

    public static boolean isUsernameTaken(String username, Integer excludeUserId) {
        return isFieldTaken("USERNAME", username, excludeUserId);
    }

    public static boolean isEmailTaken(String email) {
        return isEmailTaken(email, null);
    }

    public static boolean isEmailTaken(String email, Integer excludeUserId) {
        return isFieldTaken("EMAIL", email, excludeUserId);
    }

    public static User loginUser(String email, String password) {
        String sql = "SELECT * FROM \"USER\" WHERE LOWER(TRIM(EMAIL)) = LOWER(TRIM(?))";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                String storedPassword = rs.getString("PASSWORD");
                if (!PasswordUtil.matches(password, storedPassword)) {
                    return null;
                }

                User user = mapUser(rs);
                if (PasswordUtil.needsUpgrade(storedPassword)) {
                    String hashedPassword = PasswordUtil.hashPassword(password);
                    upgradeStoredPassword(user.getUserId(), hashedPassword);
                    user.setPassword(hashedPassword);
                }
                return user;
            }
        } catch (SQLException e) {
            logger.error("Failed to login user {}", email, e);
        }
        return null;
    }

    public static User getUserById(int userId) {
        String sql = "SELECT * FROM \"USER\" WHERE USERID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to fetch user {}", userId, e);
        }
        return null;
    }

    public static boolean updateUser(User user) {
        String sql = "UPDATE \"USER\" SET FIRSTNAME=?, LASTNAME=?, USERNAME=?, EMAIL=?, PHONENUMBER=?, PASSWORD=?, " +
                "EMAIL_KEY=?, USERNAME_KEY=? WHERE USERID=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getFirstName());
            stmt.setString(2, user.getLastName());
            stmt.setString(3, user.getUsername());
            stmt.setString(4, user.getEmail());
            stmt.setString(5, user.getPhoneNumber());
            stmt.setString(6, PasswordUtil.ensureHashed(user.getPassword()));
            stmt.setString(7, canonical(user.getEmail()));
            stmt.setString(8, canonical(user.getUsername()));
            stmt.setInt(9, user.getUserId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to update user {}", user.getUserId(), e);
        }
        return false;
    }

    public static boolean updateOnboardingState(int userId, boolean required, boolean completed, String step) {
        String sql = "UPDATE \"USER\" SET ONBOARDING_REQUIRED=?, ONBOARDING_COMPLETED=?, ONBOARDING_STEP=? WHERE USERID=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBoolean(1, required);
            stmt.setBoolean(2, completed);
            stmt.setString(3, step);
            stmt.setInt(4, userId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to update onboarding state for user {}", userId, e);
        }
        return false;
    }

    private static boolean isFieldTaken(String fieldName, String value, Integer excludeUserId) {
        String sql = "SELECT COUNT(*) FROM \"USER\" WHERE LOWER(TRIM(" + fieldName + ")) = LOWER(TRIM(?))" +
                (excludeUserId != null ? " AND USERID <> ?" : "");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, value);
            if (excludeUserId != null) {
                stmt.setInt(2, excludeUserId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to check {} duplicate", fieldName, e);
        }
        return false;
    }

    private static void upgradeStoredPassword(int userId, String hashedPassword) {
        String sql = "UPDATE \"USER\" SET PASSWORD = ? WHERE USERID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, hashedPassword);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
            logger.info("Upgraded legacy password hash for user {}", userId);
        } catch (SQLException e) {
            logger.error("Failed to upgrade password hash for user {}", userId, e);
        }
    }

    private static String canonical(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("USERID"));
        user.setFirstName(rs.getString("FIRSTNAME"));
        user.setLastName(rs.getString("LASTNAME"));
        user.setUsername(rs.getString("USERNAME"));
        user.setEmail(rs.getString("EMAIL"));
        user.setPhoneNumber(rs.getString("PHONENUMBER"));
        user.setPassword(rs.getString("PASSWORD"));
        try {
            user.setCreatedAt(rs.getTimestamp("CREATED_AT"));
        } catch (SQLException ignore) {
            // Optional column in some older schemas.
        }
        try {
            user.setOnboardingRequired(rs.getBoolean("ONBOARDING_REQUIRED"));
            user.setOnboardingCompleted(rs.getBoolean("ONBOARDING_COMPLETED"));
            user.setOnboardingStep(rs.getString("ONBOARDING_STEP"));
        } catch (SQLException ignore) {
            user.setOnboardingRequired(false);
            user.setOnboardingCompleted(true);
            user.setOnboardingStep("COMPLETE");
        }
        return user;
    }
}
