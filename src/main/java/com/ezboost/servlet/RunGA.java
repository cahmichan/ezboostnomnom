package com.ezboost.servlet;

import com.ezboost.dao.MarketSegmentDAO;
import com.ezboost.dao.OptimizationRequestDAO;
import com.ezboost.dao.OptimizationResultDAO;
import com.ezboost.dao.OptimizationRunMetadataDAO;
import com.ezboost.dao.AuditEventDAO;
import com.ezboost.dao.OptimizationReportSnapshotDAO;
import com.ezboost.dao.RoomDataDAO;
import com.ezboost.dao.SeasonalityDAO;
import com.ezboost.ga.DemandCurve;
import com.ezboost.ga.GeneticAlgorithm;
import com.ezboost.model.MarketSegment;
import com.ezboost.model.MonthlySeasonData;
import com.ezboost.model.Room;
import com.ezboost.model.Season;
import com.ezboost.model.SeasonThreshold;
import com.ezboost.model.User;
import com.ezboost.service.EventSeasonService;
import com.ezboost.service.SegmentPricingService;
import com.ezboost.service.OptimizationReportSnapshot;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RunGA extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(RunGA.class);
    private static final double BOUND_TOLERANCE = 0.01;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        logger.debug("RunGA: Starting optimization");
        HttpSession session = request.getSession(false);

        try {
            String revenueStr = request.getParameter("expectedRevenue");
            if (revenueStr == null || revenueStr.trim().isEmpty()) {
                forwardError(request, response, "Please enter a revenue target.");
                return;
            }

            double expectedRevenue = Double.parseDouble(revenueStr);
            if (expectedRevenue <= 0) {
                forwardError(request, response, "Revenue target must be greater than zero.");
                return;
            }

            if (session == null) {
                response.sendRedirect("login.jsp?error=Session expired. Please login again.");
                return;
            }

            User user = (User) session.getAttribute("user");
            if (user == null) {
                response.sendRedirect("login.jsp?error=Please login first.");
                return;
            }

            int userId = user.getUserId();
            List<Room> rooms = RoomDataDAO.getAllRooms(userId);
            List<MonthlySeasonData> monthlyData = SeasonalityDAO.getMonthlyDataByUser(userId);
            SeasonThreshold thresholds = SeasonalityDAO.getThresholdsByUser(userId);

            if (rooms.isEmpty()) {
                forwardError(request, response, "No room data found for your account. Import room data first.");
                return;
            }
            if (monthlyData.isEmpty() || thresholds == null) {
                forwardError(request, response, "Monthly demand data is incomplete. Import monthly data before running optimization.");
                return;
            }

            int requestId;
            try {
                requestId = OptimizationRequestDAO.createRequest(userId);
            } catch (RuntimeException e) {
                logger.error("Could not create optimization request for user {}", userId, e);
                forwardError(request, response, "Optimization could not be recorded. Please try again.");
                return;
            }

            DemandCurve demandCurve;
            boolean demandCurveFallback = false;
            try {
                demandCurve = DemandCurve.fitFromData(monthlyData);
                demandCurveFallback = Math.abs(demandCurve.getIntercept() - 100.0) < 0.0001
                        && Math.abs(demandCurve.getSlope() + 0.05) < 0.0001
                        && Math.abs(demandCurve.getRSquared()) < 0.0001;
            } catch (Exception e) {
                logger.warn("Could not build demand curve, using default fallback", e);
                demandCurve = DemandCurve.createDefault();
                demandCurveFallback = true;
            }

            long randomSeed = new java.security.SecureRandom().nextLong();
            GeneticAlgorithm ga = new GeneticAlgorithm(expectedRevenue, rooms, userId, demandCurve, randomSeed);
            List<Room> optimizedSolution = ga.runGA();

            if (optimizedSolution == null || optimizedSolution.isEmpty()) {
                forwardError(request, response, "Optimization failed. No pricing solution was generated.");
                return;
            }

            double achievedRevenue = optimizedSolution.stream()
                    .mapToDouble(Room::getEstimatedRevenue)
                    .sum();
            double accuracy = 100 - (Math.abs(achievedRevenue - expectedRevenue) / expectedRevenue * 100);
            accuracy = Math.max(0, accuracy);

            SegmentPricingService segmentService = new SegmentPricingService();
            List<MarketSegment> segments = segmentService.getSegments(userId);
            if (segments == null || segments.isEmpty()) {
                MarketSegmentDAO.initializeDefaultSegments(userId);
                segments = segmentService.getSegments(userId);
            }
            List<Map<String, Object>> pricingTable = segmentService.generatePricingTable(optimizedSolution, userId);

            List<Map<String, Object>> monthlyForecast = null;
            int forecastYear = Calendar.getInstance().get(Calendar.YEAR);
            try {
                EventSeasonService eventService = new EventSeasonService();
                monthlyForecast = eventService.generateMonthlyForecast(optimizedSolution, userId, forecastYear, demandCurve);
            } catch (Exception e) {
                logger.warn("Could not generate event forecast", e);
            }

            double[] achievableRange = ga.getAchievableRange();
            int eventAdjustedMonthCount = countAdjustedMonths(monthlyForecast);
            List<String> constraintHighlights = buildConstraintHighlights(ga, optimizedSolution);
            Map<String, String> priceConstraintStates = buildConstraintStates(ga, optimizedSolution);

            String persistenceWarning = null;
            try {
                OptimizationResultDAO.saveResult(requestId, optimizedSolution, achievedRevenue);
                OptimizationRunMetadataDAO.save(requestId, userId, expectedRevenue, achievedRevenue, randomSeed,
                        demandCurveFallback ? "Fallback default curve" : "Historical fit");
                OptimizationReportSnapshotDAO.save(requestId, userId,
                        new OptimizationReportSnapshot(optimizedSolution, expectedRevenue, achievedRevenue, segments,
                                monthlyForecast, forecastYear,
                                demandCurveFallback ? "Fallback default curve" : "Historical fit",
                                achievableRange[0], achievableRange[1], constraintHighlights).toJson());
                AuditEventDAO.record(userId, "OPTIMIZATION_RUN", "OptimizationRequest", "SUCCESS");
            } catch (Exception dbError) {
                persistenceWarning = "Optimization completed, but the result could not be saved to history.";
                logger.warn("Could not save optimization result for request {}", requestId, dbError);
            }

            request.setAttribute("requestId", requestId);
            request.setAttribute("expectedRevenue", expectedRevenue);
            request.setAttribute("estimatedRevenue", achievedRevenue);
            request.setAttribute("bestSolution", optimizedSolution);
            request.setAttribute("accuracy", accuracy);
            request.setAttribute("marketSegments", segments);
            request.setAttribute("pricingTable", pricingTable);
            request.setAttribute("userMultipliers", ga.getUserMultipliers());
            request.setAttribute("achievableMinRevenue", achievableRange[0]);
            request.setAttribute("achievableMaxRevenue", achievableRange[1]);
            request.setAttribute("targetDifficulty", describeTargetDifficulty(expectedRevenue, achievableRange));
            request.setAttribute("demandCurveMode", demandCurveFallback ? "Fallback default curve" : "Historical fit");
            request.setAttribute("demandCurveSummary", buildDemandCurveSummary(demandCurve, demandCurveFallback));
            request.setAttribute("constraintHighlights", constraintHighlights);
            request.setAttribute("priceConstraintStates", priceConstraintStates);
            request.setAttribute("eventAdjustedMonthCount", eventAdjustedMonthCount);
            request.setAttribute("optimizationWarning", persistenceWarning);
            request.setAttribute("optimizationSeed", randomSeed);

            if (monthlyForecast != null) {
                request.setAttribute("monthlyForecast", monthlyForecast);
                request.setAttribute("forecastYear", forecastYear);
                session.setAttribute("monthlyForecast", monthlyForecast);
                session.setAttribute("forecastYear", forecastYear);
            }

            session.setAttribute("requestId", requestId);
            session.setAttribute("expectedRevenue", expectedRevenue);
            session.setAttribute("estimatedRevenue", achievedRevenue);
            session.setAttribute("bestSolution", optimizedSolution);
            session.setAttribute("marketSegments", segments);
            session.setAttribute("demandCurve", demandCurve);
            session.setAttribute("achievableMinRevenue", achievableRange[0]);
            session.setAttribute("achievableMaxRevenue", achievableRange[1]);
            session.setAttribute("constraintHighlights", constraintHighlights);
            session.setAttribute("demandCurveMode", demandCurveFallback ? "Fallback default curve" : "Historical fit");
            session.setAttribute("optimizationSeed", randomSeed);

            RequestDispatcher rd = request.getRequestDispatcher("BoostMe.jsp");
            rd.forward(request, response);
        } catch (NumberFormatException e) {
            logger.error("Invalid revenue format", e);
            forwardError(request, response, "Invalid revenue value. Please enter a valid number.");
        } catch (Exception e) {
            logger.error("Exception in RunGA", e);
            forwardError(request, response, "An error occurred during optimization: " + e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect("BoostMe.jsp");
    }

    private void forwardError(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        request.setAttribute("error", message);
        request.getRequestDispatcher("BoostMe.jsp").forward(request, response);
    }

    private String describeTargetDifficulty(double targetRevenue, double[] achievableRange) {
        if (targetRevenue < achievableRange[0]) {
            return "Below current achievable range";
        }
        if (targetRevenue > achievableRange[1]) {
            return "Above current achievable range";
        }

        double midpoint = (achievableRange[0] + achievableRange[1]) / 2.0;
        double distanceFromMid = Math.abs(targetRevenue - midpoint);
        double halfRange = Math.max(1.0, (achievableRange[1] - achievableRange[0]) / 2.0);
        double normalized = distanceFromMid / halfRange;

        if (normalized < 0.25) {
            return "Comfortable";
        }
        if (normalized < 0.65) {
            return "Moderate";
        }
        return "Tight";
    }

    private String buildDemandCurveSummary(DemandCurve demandCurve, boolean demandCurveFallback) {
        if (demandCurveFallback) {
            return "Using the default price-demand curve because the historical fit was weak or unavailable.";
        }
        return String.format("Using historical fit: optimal room-night price near RM %.0f with R² %.2f.",
                demandCurve.getOptimalPrice(), demandCurve.getRSquared());
    }

    private int countAdjustedMonths(List<Map<String, Object>> monthlyForecast) {
        if (monthlyForecast == null) {
            return 0;
        }

        int count = 0;
        for (Map<String, Object> month : monthlyForecast) {
            if (Boolean.TRUE.equals(month.get("seasonChanged"))) {
                count++;
            }
        }
        return count;
    }

    private List<String> buildConstraintHighlights(GeneticAlgorithm ga, List<Room> optimizedSolution) {
        List<String> highlights = new ArrayList<>();

        for (Room room : optimizedSolution) {
            for (Season season : Season.values()) {
                double price = room.getSeasonalPrices().get(season);
                double min = ga.getSeasonalMinPriceForDisplay(room, season);
                double max = ga.getSeasonalMaxPriceForDisplay(room, season);

                if (Math.abs(price - min) <= BOUND_TOLERANCE) {
                    highlights.add(room.getName() + " / " + season.name() + " is sitting on its floor price.");
                } else if (Math.abs(price - max) <= BOUND_TOLERANCE) {
                    highlights.add(room.getName() + " / " + season.name() + " is sitting on its ceiling price.");
                }

                if (highlights.size() >= 8) {
                    return highlights;
                }
            }
        }

        if (highlights.isEmpty()) {
            highlights.add("No room-season price landed exactly on a floor or ceiling constraint.");
        }
        return highlights;
    }

    private Map<String, String> buildConstraintStates(GeneticAlgorithm ga, List<Room> optimizedSolution) {
        Map<String, String> states = new HashMap<>();

        for (Room room : optimizedSolution) {
            for (Season season : Season.values()) {
                double price = room.getSeasonalPrices().get(season);
                double min = ga.getSeasonalMinPriceForDisplay(room, season);
                double max = ga.getSeasonalMaxPriceForDisplay(room, season);

                String key = room.getName() + "|" + season.name();
                if (Math.abs(price - min) <= BOUND_TOLERANCE) {
                    states.put(key, "FLOOR");
                } else if (Math.abs(price - max) <= BOUND_TOLERANCE) {
                    states.put(key, "CEILING");
                } else {
                    states.put(key, "FREE");
                }
            }
        }

        return states;
    }
}
