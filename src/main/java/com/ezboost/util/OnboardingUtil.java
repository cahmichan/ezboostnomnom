package com.ezboost.util;

import com.ezboost.dao.UserDAO;
import com.ezboost.model.User;
import jakarta.servlet.http.HttpSession;

import java.util.Arrays;
import java.util.List;

public final class OnboardingUtil {

    public static final String STEP_IMPORT = "IMPORT";
    public static final String STEP_MULTIPLIERS = "MULTIPLIERS";
    public static final String STEP_EVENTS = "EVENTS";
    public static final String STEP_SEGMENTS = "SEGMENTS";
    public static final String STEP_COMPLETE = "COMPLETE";

    public static final List<String> STEP_ORDER = Arrays.asList(
            STEP_IMPORT,
            STEP_MULTIPLIERS,
            STEP_EVENTS,
            STEP_SEGMENTS
    );

    private OnboardingUtil() {
    }

    public static boolean isOnboardingActive(User user) {
        return user != null && user.isOnboardingRequired() && !user.isOnboardingCompleted();
    }

    public static String normalizeStep(String step) {
        if (step == null || step.trim().isEmpty()) {
            return STEP_COMPLETE;
        }

        String normalized = step.trim().toUpperCase();
        return STEP_ORDER.contains(normalized) || STEP_COMPLETE.equals(normalized)
                ? normalized
                : STEP_IMPORT;
    }

    public static String getCurrentStep(User user) {
        if (!isOnboardingActive(user)) {
            return STEP_COMPLETE;
        }
        String step = normalizeStep(user.getOnboardingStep());
        return STEP_COMPLETE.equals(step) ? STEP_IMPORT : step;
    }

    public static String getStepUrl(String step) {
        switch (normalizeStep(step)) {
            case STEP_IMPORT:
                return "DataImport";
            case STEP_MULTIPLIERS:
                return "MultiplierSettings";
            case STEP_EVENTS:
                return "EventSettings";
            case STEP_SEGMENTS:
                return "SegmentSettings";
            default:
                return "BoostMe.jsp";
        }
    }

    public static String getStepLabel(String step) {
        switch (normalizeStep(step)) {
            case STEP_IMPORT:
                return "Import";
            case STEP_MULTIPLIERS:
                return "Multipliers";
            case STEP_EVENTS:
                return "Events";
            case STEP_SEGMENTS:
                return "Segments";
            default:
                return "Complete";
        }
    }

    public static boolean isAllowedDuringOnboarding(String path, User user) {
        if (!isOnboardingActive(user)) {
            return true;
        }

        if (isAlwaysAllowed(path)) {
            return true;
        }

        return isPathForStep(path, getCurrentStep(user));
    }

    public static boolean isPathForStep(String path, String step) {
        switch (normalizeStep(step)) {
            case STEP_IMPORT:
                return path.equals("/DataImport") || path.equals("/data-import.jsp");
            case STEP_MULTIPLIERS:
                return path.equals("/MultiplierSettings") || path.equals("/multiplier-settings.jsp");
            case STEP_EVENTS:
                return path.equals("/EventSettings") || path.equals("/event-settings.jsp");
            case STEP_SEGMENTS:
                return path.equals("/SegmentSettings") || path.equals("/segment-settings.jsp");
            default:
                return false;
        }
    }

    public static boolean isAlwaysAllowed(String path) {
        return path.equals("/about.jsp")
                || path.equals("/LogoutServlet")
                || path.equals("/login.jsp")
                || path.equals("/register.jsp")
                || path.equals("/LoginServlet")
                || path.equals("/RegisterServlet")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/");
    }

    public static boolean advanceToStep(int userId, String nextStep, HttpSession session) {
        boolean updated = UserDAO.updateOnboardingState(userId, true, false, normalizeStep(nextStep));
        if (updated) {
            refreshSessionUser(userId, session);
        }
        return updated;
    }

    public static boolean completeOnboarding(int userId, HttpSession session) {
        boolean updated = UserDAO.updateOnboardingState(userId, false, true, STEP_COMPLETE);
        if (updated) {
            refreshSessionUser(userId, session);
        }
        return updated;
    }

    public static void refreshSessionUser(int userId, HttpSession session) {
        if (session == null) {
            return;
        }
        User refreshed = UserDAO.getUserById(userId);
        if (refreshed != null) {
            session.setAttribute("user", refreshed);
            session.setAttribute("userId", refreshed.getUserId());
        }
    }
}
