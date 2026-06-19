package com.ezboost.servlet;

import com.ezboost.dao.MarketSegmentDAO;
import com.ezboost.dao.AuditEventDAO;
import com.ezboost.model.MarketSegment;
import com.ezboost.model.User;
import com.ezboost.util.OnboardingUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * MarketSegmentSettingsServlet - Manages market segment configuration
 *
 * Each user has their own set of market segments (per-user isolation).
 *
 * Allows users to:
 * - View all market segments
 * - Edit segment multipliers and descriptions
 * - Add new segments
 * - Delete segments
 * - Reset to defaults
 */
public class MarketSegmentSettingsServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(MarketSegmentSettingsServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        User user = (User) session.getAttribute("user");
        int userId = user.getUserId();

        // Load segments for this user
        List<MarketSegment> segments = MarketSegmentDAO.getAllSegments(userId);

        // Initialize defaults if user has no segments yet
        if (segments.isEmpty()) {
            MarketSegmentDAO.initializeDefaultSegments(userId);
            segments = MarketSegmentDAO.getAllSegments(userId);
        }

        request.setAttribute("segments", segments);
        request.setAttribute("onboardingActive", OnboardingUtil.isOnboardingActive(user));

        request.getRequestDispatcher("segment-settings.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        User user = (User) session.getAttribute("user");
        int userId = user.getUserId();

        String action = request.getParameter("action");

        try {
            switch (action) {
                case "updateSegments":
                    updateSegments(request, userId);
                    AuditEventDAO.record(userId, "SEGMENT_UPDATE", "MarketSegment", "SUCCESS");
                    request.setAttribute("success", "Market segments updated successfully!");
                    if (OnboardingUtil.STEP_SEGMENTS.equals(OnboardingUtil.getCurrentStep(user))) {
                        OnboardingUtil.completeOnboarding(userId, session);
                        response.sendRedirect("BoostMe");
                        return;
                    }
                    break;

                case "addSegment":
                    addSegment(request, userId);
                    AuditEventDAO.record(userId, "SEGMENT_CREATE", "MarketSegment", "SUCCESS");
                    request.setAttribute("success", "New segment added successfully!");
                    break;

                case "deleteSegment":
                    deleteSegment(request, userId);
                    AuditEventDAO.record(userId, "SEGMENT_DELETE", "MarketSegment", "SUCCESS");
                    request.setAttribute("success", "Segment deleted successfully!");
                    break;

                case "resetDefaults":
                    resetToDefaults(userId);
                    AuditEventDAO.record(userId, "SEGMENT_RESET", "MarketSegment", "SUCCESS");
                    request.setAttribute("success", "Segments reset to default values!");
                    break;

                default:
                    request.setAttribute("error", "Unknown action: " + action);
            }
        } catch (Exception e) {
            request.setAttribute("error", e instanceof IllegalArgumentException
                    ? e.getMessage() : "Market segments could not be updated. Please try again.");
            logger.error("Failed to update market segment settings for user {}", userId, e);
        }

        // Reload and forward
        doGet(request, response);
    }

    /**
     * Update existing segments with new multipliers and descriptions
     */
    private void updateSegments(HttpServletRequest request, int userId) {
        List<MarketSegment> segments = MarketSegmentDAO.getAllSegments(userId);

        for (MarketSegment segment : segments) {
            String code = segment.getSegmentCode();

            // Get multiplier
            String multParam = request.getParameter("multiplier_" + code);
            if (multParam != null && !multParam.isEmpty()) {
                double multiplier = Double.parseDouble(multParam);
                // Validate range 0.5 to 2.0
                multiplier = Math.max(0.5, Math.min(2.0, multiplier));
                segment.setRateMultiplier(multiplier);
            }

            // Get description
            String descParam = request.getParameter("desc_" + code);
            if (descParam != null) {
                segment.setDescription(descParam);
            }

            // Save (userId is already set on the segment from getAllSegments)
            MarketSegmentDAO.saveSegment(segment);
        }
    }

    /**
     * Add a new market segment for this user
     */
    private void addSegment(HttpServletRequest request, int userId) {
        String name = request.getParameter("newName");
        String code = request.getParameter("newCode");
        String category = request.getParameter("newCategory");
        double multiplier = Double.parseDouble(request.getParameter("newMultiplier"));
        String description = request.getParameter("newDescription");

        // Validate
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Segment name is required");
        }
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Segment code is required");
        }

        // Check if code already exists for this user
        String normalizedCode = code.toUpperCase().trim();
        MarketSegment existing = MarketSegmentDAO.getSegmentByCode(normalizedCode, userId);
        if (existing != null) {
            throw new IllegalArgumentException("Segment code '" + code + "' already exists");
        }

        // Create new segment
        MarketSegment segment = new MarketSegment();
        segment.setUserId(userId);
        segment.setSegmentName(name.trim());
        segment.setSegmentCode(normalizedCode);
        segment.setCategory(category);
        segment.setRateMultiplier(Math.max(0.5, Math.min(2.0, multiplier)));
        segment.setDescription(description);
        segment.setActive(true);

        if (!MarketSegmentDAO.saveSegment(segment)) {
            throw new IllegalStateException("Segment could not be saved. The segment code may already exist in the database.");
        }
    }

    /**
     * Delete (deactivate) a segment for this user
     */
    private void deleteSegment(HttpServletRequest request, int userId) {
        String code = request.getParameter("segmentCode");
        if (code != null && !code.isEmpty()) {
            MarketSegmentDAO.deleteSegment(code, userId);
        }
    }

    /**
     * Reset all segments to default values for this user
     */
    private void resetToDefaults(int userId) {
        // Delete all segments for this user
        MarketSegmentDAO.deleteAllSegments(userId);

        // Reinitialize with defaults for this user
        MarketSegmentDAO.initializeDefaultSegments(userId);
    }
}
