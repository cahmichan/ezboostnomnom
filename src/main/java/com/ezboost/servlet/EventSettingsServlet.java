package com.ezboost.servlet;

import com.ezboost.dao.FutureEventDAO;
import com.ezboost.model.FutureEvent;
import com.ezboost.model.User;
import com.ezboost.service.CalendarificService;
import com.ezboost.util.OnboardingUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Date;
import java.util.List;

public class EventSettingsServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(EventSettingsServlet.class);

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

        List<FutureEvent> events = FutureEventDAO.getAllEventsIncludingInactive(userId);
        String apiKey = FutureEventDAO.getApiKey(userId);

        request.setAttribute("events", events);
        request.setAttribute("apiKeyConfigured", apiKey != null && !apiKey.trim().isEmpty());
        request.setAttribute("onboardingActive", OnboardingUtil.isOnboardingActive(user));
        setEventSummaryAttributes(request, events);

        request.getRequestDispatcher("event-settings.jsp").forward(request, response);
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
        boolean ajax = isAjaxRequest(request);

        try {
            switch (action != null ? action : "") {
                case "saveApiKey":
                    handleSaveApiKey(request, userId);
                    request.setAttribute("success", "API key saved successfully!");
                    break;
                case "fetchHolidays":
                    request.setAttribute("success", handleFetchHolidays(request, userId));
                    break;
                case "loadSchoolHolidays":
                    request.setAttribute("success", handleLoadSchoolHolidays(request, userId));
                    break;
                case "addEvent":
                    handleAddEvent(request, userId);
                    request.setAttribute("success", "Custom event added successfully!");
                    break;
                case "updateEvent":
                    handleUpdateEvent(request, userId);
                    if (ajax) {
                        writeJson(response, HttpServletResponse.SC_OK,
                                "{\"success\":true,\"message\":\"Event updated successfully.\"}");
                        return;
                    }
                    request.setAttribute("success", "Event updated successfully!");
                    break;
                case "deleteEvent":
                    handleDeleteEvent(request, userId);
                    request.setAttribute("success", "Event deleted successfully!");
                    break;
                case "deleteAll":
                    FutureEventDAO.deleteAllEvents(userId);
                    request.setAttribute("success", "All events deleted successfully!");
                    break;
                case "continueOnboardingEvents":
                    if (OnboardingUtil.STEP_EVENTS.equals(OnboardingUtil.getCurrentStep(user))) {
                        OnboardingUtil.advanceToStep(userId, OnboardingUtil.STEP_SEGMENTS, session);
                    }
                    response.sendRedirect("SegmentSettings");
                    return;
                default:
                    if (ajax) {
                        writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                                "{\"success\":false,\"message\":\"Unknown action.\"}");
                        return;
                    }
                    request.setAttribute("error", "Unknown action: " + action);
            }
        } catch (Exception e) {
            logger.error("Error processing event action", e);
            if (ajax) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                        "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
                return;
            }
            request.setAttribute("error", "Error: " + e.getMessage());
        }

        doGet(request, response);
    }

    private void handleSaveApiKey(HttpServletRequest request, int userId) {
        String apiKey = request.getParameter("apiKey");
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            FutureEventDAO.saveApiKey(userId, apiKey.trim());
        }
    }

    private String handleFetchHolidays(HttpServletRequest request, int userId) throws Exception {
        int year = Integer.parseInt(request.getParameter("year"));
        String apiKey = FutureEventDAO.getApiKey(userId);
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new Exception("Please save your Calendarific API key first.");
        }

        int replacedCount = countEventsBySourceYear(userId, "CALENDARIFIC", year);
        CalendarificService service = new CalendarificService();
        List<FutureEvent> holidays = service.fetchHolidays(apiKey, year, userId);

        FutureEventDAO.deleteEventsBySourceAndYear(userId, "CALENDARIFIC", year);
        FutureEventDAO.batchSaveEvents(holidays);

        logger.debug("Fetched and saved {} holidays for {}", holidays.size(), year);
        return "Loaded " + holidays.size() + " API holiday events for " + year +
                " and replaced " + replacedCount + " earlier API rows for that same year.";
    }

    private String handleLoadSchoolHolidays(HttpServletRequest request, int userId) {
        int year = Integer.parseInt(request.getParameter("year"));

        int replacedCount = countEventsBySourceYear(userId, "PRESET", year);
        CalendarificService service = new CalendarificService();
        List<FutureEvent> schoolHolidays = service.getSchoolHolidayPresets(year, userId);

        FutureEventDAO.deleteEventsBySourceAndYear(userId, "PRESET", year);
        FutureEventDAO.batchSaveEvents(schoolHolidays);

        logger.debug("Loaded {} school holiday presets for {}", schoolHolidays.size(), year);
        return "Loaded " + schoolHolidays.size() + " preset school-break events for " + year +
                " and replaced " + replacedCount + " earlier preset rows for that same year.";
    }

    private void handleAddEvent(HttpServletRequest request, int userId) {
        String name = request.getParameter("eventName");
        String dateStr = request.getParameter("eventDate");
        String endDateStr = request.getParameter("eventEndDate");
        String eventType = request.getParameter("eventType");
        String seasonOverride = request.getParameter("seasonOverride");

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Event name is required");
        }
        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Event date is required");
        }

        FutureEvent event = new FutureEvent();
        event.setUserId(userId);
        event.setEventName(name.trim());
        event.setEventDate(Date.valueOf(dateStr));
        if (endDateStr != null && !endDateStr.trim().isEmpty()) {
            event.setEventEndDate(Date.valueOf(endDateStr));
        }
        event.setEventType(eventType != null ? eventType : "CUSTOM");
        event.setSeasonOverride(seasonOverride != null ? seasonOverride : "PEAK");
        event.setSource("MANUAL");
        event.setActive(true);

        FutureEventDAO.saveEvent(event);
    }

    private void handleUpdateEvent(HttpServletRequest request, int userId) {
        int eventId = Integer.parseInt(request.getParameter("eventId"));
        String seasonOverride = request.getParameter("seasonOverride");
        String activeStr = request.getParameter("active");

        List<FutureEvent> allEvents = FutureEventDAO.getAllEventsIncludingInactive(userId);
        FutureEvent target = null;
        for (FutureEvent event : allEvents) {
            if (event.getEventId() == eventId) {
                target = event;
                break;
            }
        }

        if (target == null) {
            throw new IllegalArgumentException("Event not found");
        }

        if (seasonOverride != null) {
            target.setSeasonOverride(seasonOverride);
        }
        if (activeStr != null) {
            target.setActive("true".equals(activeStr));
        }

        FutureEventDAO.updateEvent(target);
    }

    private void handleDeleteEvent(HttpServletRequest request, int userId) {
        int eventId = Integer.parseInt(request.getParameter("eventId"));
        FutureEventDAO.deleteEvent(eventId, userId);
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith);
    }

    private int countEventsBySourceYear(int userId, String source, int year) {
        int count = 0;
        for (FutureEvent event : FutureEventDAO.getAllEventsIncludingInactive(userId)) {
            if (source.equals(event.getSource())
                    && event.getEventDate() != null
                    && event.getEventDate().toLocalDate().getYear() == year) {
                count++;
            }
        }
        return count;
    }

    private void setEventSummaryAttributes(HttpServletRequest request, List<FutureEvent> events) {
        int apiCount = 0;
        int presetCount = 0;
        int manualCount = 0;
        int activeCount = 0;

        for (FutureEvent event : events) {
            if (event.isActive()) {
                activeCount++;
            }
            String source = event.getSource() != null ? event.getSource() : "MANUAL";
            switch (source) {
                case "CALENDARIFIC":
                    apiCount++;
                    break;
                case "PRESET":
                    presetCount++;
                    break;
                default:
                    manualCount++;
                    break;
            }
        }

        request.setAttribute("activeEventCount", activeCount);
        request.setAttribute("apiEventCount", apiCount);
        request.setAttribute("presetEventCount", presetCount);
        request.setAttribute("manualEventCount", manualCount);
    }

    private void writeJson(HttpServletResponse response, int status, String body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(body);
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "Unexpected error";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
