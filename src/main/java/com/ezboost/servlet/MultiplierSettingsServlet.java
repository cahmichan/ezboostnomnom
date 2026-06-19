package com.ezboost.servlet;

import com.ezboost.dao.RoomDataDAO;
import com.ezboost.dao.UserSettingsDAO;
import com.ezboost.dao.AuditEventDAO;
import com.ezboost.model.Room;
import com.ezboost.model.User;
import com.ezboost.model.UserMultiplierSettings;
import com.ezboost.util.OnboardingUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MultiplierSettingsServlet - Manages user's seasonal price multipliers
 * 
 * These multipliers affect the achievable revenue range:
 * - Higher multipliers = higher potential revenue
 * - Lower multipliers = lower potential revenue
 * 
 * Default multipliers:
 * - LOW: 0.85 (15% discount during low season)
 * - NORMAL: 1.0 (standard pricing)
 * - PEAK: 1.15 (15% premium during peak)
 * - SUPER_PEAK: 1.35 (35% premium during super peak)
 * 
 * NOTE: URL mapping (/MultiplierSettings) is done in web.xml
 */
public class MultiplierSettingsServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(MultiplierSettingsServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect("login.jsp?error=Please login first");
            return;
        }

        User user = (User) session.getAttribute("user");
        int userId = user.getUserId();

        // Load current settings
        loadUserSettings(request, userId);

        request.getRequestDispatcher("multiplier-settings.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect("login.jsp?error=Please login first");
            return;
        }

        User user = (User) session.getAttribute("user");
        int userId = user.getUserId();

        String action = request.getParameter("action");
        logger.debug("[MultiplierSettingsServlet] Action: {}, UserID: {}", action, userId);

        try {
            switch (action != null ? action : "") {
                case "updateMultipliers":
                    int updatedCount = handleUpdateMultipliers(request, userId);
                    AuditEventDAO.record(userId, "MULTIPLIER_UPDATE", "UserMultiplierSettings", "SUCCESS");
                    if (updatedCount > 0
                            && OnboardingUtil.STEP_MULTIPLIERS.equals(OnboardingUtil.getCurrentStep(user))) {
                        OnboardingUtil.advanceToStep(userId, OnboardingUtil.STEP_EVENTS, session);
                        response.sendRedirect("EventSettings");
                        return;
                    }
                    break;
                case "resetDefaults":
                    handleResetDefaults(userId);
                    AuditEventDAO.record(userId, "MULTIPLIER_RESET", "UserMultiplierSettings", "SUCCESS");
                    request.setAttribute("success", "Multipliers reset to defaults!");
                    break;
                default:
                    request.setAttribute("error", "Unknown action: " + action);
            }
        } catch (Exception e) {
            logger.error("[MultiplierSettingsServlet] Error: {}", e.getMessage(), e);
            request.setAttribute("error", e instanceof IllegalArgumentException
                    ? e.getMessage() : "Multiplier settings could not be updated. Please try again.");
        }

        // Reload settings and display
        loadUserSettings(request, userId);
        request.getRequestDispatcher("multiplier-settings.jsp").forward(request, response);
    }

    /**
     * Load user's multiplier settings
     */
    private void loadUserSettings(HttpServletRequest request, int userId) {
        List<UserMultiplierSettings> allSettings = UserSettingsDAO.getUserSettings(userId);

        // If no settings exist, initialize defaults
        if (allSettings.isEmpty()) {
            logger.debug("[MultiplierSettingsServlet] No settings found, initializing defaults");
            UserSettingsDAO.initializeDefaultSettings(userId);
            allSettings = UserSettingsDAO.getUserSettings(userId);
        }

        // Organize settings by season (global multipliers only)
        Map<String, UserMultiplierSettings> seasonMultipliers = new HashMap<>();
        for (UserMultiplierSettings setting : allSettings) {
            // Only include global season multipliers (no specific room or segment)
            if (setting.getRoomType() == null && setting.getSegmentName() == null) {
                seasonMultipliers.put(setting.getSeasonName(), setting);
            }
        }

        // Ensure all seasons have a setting
        String[] seasons = {"LOW", "NORMAL", "PEAK", "SUPER_PEAK"};
        double[] defaults = {0.85, 1.0, 1.15, 1.35};

        for (int i = 0; i < seasons.length; i++) {
            if (!seasonMultipliers.containsKey(seasons[i])) {
                UserMultiplierSettings defaultSetting = new UserMultiplierSettings();
                defaultSetting.setUserId(userId);
                defaultSetting.setSeasonName(seasons[i]);
                defaultSetting.setCustomMultiplier(defaults[i]);
                defaultSetting.setMinBound(0.5);
                defaultSetting.setMaxBound(2.0);
                defaultSetting.setLocked(false);
                seasonMultipliers.put(seasons[i], defaultSetting);
            }
        }

        // Calculate impact preview (how much revenue range changes)
        double lowMult = seasonMultipliers.get("LOW").getCustomMultiplier();
        double normalMult = seasonMultipliers.get("NORMAL").getCustomMultiplier();
        double peakMult = seasonMultipliers.get("PEAK").getCustomMultiplier();
        double superPeakMult = seasonMultipliers.get("SUPER_PEAK").getCustomMultiplier();
        double avgMultiplier = (lowMult + normalMult + peakMult + superPeakMult) / 4.0;

        request.setAttribute("seasonMultipliers", seasonMultipliers);
        request.setAttribute("allSettings", allSettings);
        request.setAttribute("avgMultiplier", avgMultiplier);
        request.setAttribute("previewBaseRate", RoomDataDAO.getWeightedAverageBaseRate(userId));
        request.setAttribute("onboardingActive", OnboardingUtil.isOnboardingActive((User) request.getSession().getAttribute("user")));

        List<Room> previewRooms = RoomDataDAO.getAllRooms(userId);
        if (!previewRooms.isEmpty()) {
            request.setAttribute("previewRoomA", previewRooms.get(0));
            if (previewRooms.size() > 1) {
                request.setAttribute("previewRoomB", previewRooms.get(1));
            }
        }
        
        logger.debug("[MultiplierSettingsServlet] Loaded {} season multipliers", seasonMultipliers.size());
    }

    /**
     * Handle multiplier updates from form
     */
    private int handleUpdateMultipliers(HttpServletRequest request, int userId) {
        String[] seasons = {"LOW", "NORMAL", "PEAK", "SUPER_PEAK"};
        int updatedCount = 0;

        for (String season : seasons) {
            String paramName = "multiplier_" + season;
            String value = request.getParameter(paramName);

            if (value != null && !value.isEmpty()) {
                try {
                    double multiplier = Double.parseDouble(value);

                    // Validate bounds (0.5 to 2.0)
                    if (multiplier < 0.5) {
                        multiplier = 0.5;
                        logger.debug("[MultiplierSettings] {} clamped to minimum 0.5", season);
                    }
                    if (multiplier > 2.0) {
                        multiplier = 2.0;
                        logger.debug("[MultiplierSettings] {} clamped to maximum 2.0", season);
                    }

                    // Create or update setting
                    UserMultiplierSettings setting = new UserMultiplierSettings();
                    setting.setUserId(userId);
                    setting.setRoomType(null);  // Global setting
                    setting.setSeasonName(season);
                    setting.setSegmentName(null);  // Global setting
                    setting.setCustomMultiplier(multiplier);
                    setting.setMinBound(0.5);
                    setting.setMaxBound(2.0);
                    setting.setLocked(false);

                    int settingId = UserSettingsDAO.saveMultiplierSetting(setting);
                    if (settingId > 0) {
                        updatedCount++;
                        logger.debug("Updated {} to {}", season, multiplier);
                    }

                } catch (NumberFormatException e) {
                    logger.warn("Invalid value for {}: {}", season, value);
                }
            }
        }

        request.setAttribute("success", "Updated " + updatedCount + " multiplier settings!");
        return updatedCount;
    }

    /**
     * Reset multipliers to default values
     */
    private void handleResetDefaults(int userId) {
        UserSettingsDAO.deleteAllUserSettings(userId);
        UserSettingsDAO.initializeDefaultSettings(userId);
        logger.debug("Reset to defaults for user {}", userId);
    }
}
