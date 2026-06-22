package com.ezboost.servlet;

import com.ezboost.dao.AuditEventDAO;
import com.ezboost.dao.MarketSegmentDAO;
import com.ezboost.dao.OptimizationReportSnapshotDAO;
import com.ezboost.model.MarketSegment;
import com.ezboost.model.Room;
import com.ezboost.model.User;
import com.ezboost.service.ExcelExportService;
import com.ezboost.service.OptimizationReportSnapshot;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DownloadReportServlet extends HttpServlet {

    private final ExcelExportService excelExportService = new ExcelExportService();

    @SuppressWarnings("unchecked")
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No session found.");
            return;
        }

        User user = (User) session.getAttribute("user");
        if (user == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Please log in again.");
            return;
        }

        String requestIdParameter = request.getParameter("requestId");
        if (requestIdParameter != null && !requestIdParameter.trim().isEmpty()) {
            exportSnapshot(requestIdParameter, user, response);
            return;
        }

        List<Room> rooms = (List<Room>) session.getAttribute("bestSolution");
        Double expectedRevenue = (Double) session.getAttribute("expectedRevenue");
        Double estimatedRevenue = (Double) session.getAttribute("estimatedRevenue");

        if (rooms == null || expectedRevenue == null || estimatedRevenue == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No optimization results found.");
            return;
        }

        List<MarketSegment> marketSegments = (List<MarketSegment>) session.getAttribute("marketSegments");
        if (marketSegments == null || marketSegments.isEmpty()) {
            marketSegments = MarketSegmentDAO.getAllSegments(user.getUserId());
            if (marketSegments == null || marketSegments.isEmpty()) {
                MarketSegmentDAO.initializeDefaultSegments(user.getUserId());
                marketSegments = MarketSegmentDAO.getAllSegments(user.getUserId());
            }
        }

        List<Map<String, Object>> monthlyForecast =
                (List<Map<String, Object>>) session.getAttribute("monthlyForecast");
        Integer forecastYear = (Integer) session.getAttribute("forecastYear");
        String demandCurveMode = (String) session.getAttribute("demandCurveMode");
        Double achievableMinRevenue = (Double) session.getAttribute("achievableMinRevenue");
        Double achievableMaxRevenue = (Double) session.getAttribute("achievableMaxRevenue");
        List<String> constraintHighlights = (List<String>) session.getAttribute("constraintHighlights");

        byte[] reportBytes = excelExportService.generateReport(
                rooms,
                expectedRevenue,
                estimatedRevenue,
                marketSegments,
                monthlyForecast,
                forecastYear,
                demandCurveMode != null ? demandCurveMode : "Historical fit",
                achievableMinRevenue,
                achievableMaxRevenue,
                constraintHighlights != null ? constraintHighlights : Collections.emptyList()
        );

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=EzBoost_Optimization_Report.xlsx");

        try (OutputStream out = response.getOutputStream()) {
            out.write(reportBytes);
        }
        AuditEventDAO.record(user.getUserId(), "REPORT_EXPORT", "OptimizationReport", "SUCCESS");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Use POST to export reports.");
    }

    private void exportSnapshot(String requestIdParameter, User user, HttpServletResponse response) throws IOException {
        final int requestId;
        try {
            requestId = Integer.parseInt(requestIdParameter);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid optimization request ID.");
            return;
        }
        String payload = OptimizationReportSnapshotDAO.load(requestId, user.getUserId());
        if (payload == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No saved report exists for this optimization.");
            return;
        }
        try {
            OptimizationReportSnapshot snapshot = OptimizationReportSnapshot.fromJson(payload);
            byte[] reportBytes = excelExportService.generateReport(snapshot.getRooms(), snapshot.getTargetRevenue(),
                    snapshot.getEstimatedRevenue(), snapshot.getMarketSegments(), snapshot.getMonthlyForecast(),
                    snapshot.getForecastYear(), snapshot.getDemandCurveMode(), snapshot.getAchievableMinRevenue(),
                    snapshot.getAchievableMaxRevenue(), snapshot.getConstraintHighlights());
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=EzBoost_Optimization_Report_" + requestId + ".xlsx");
            try (OutputStream out = response.getOutputStream()) {
                out.write(reportBytes);
            }
            AuditEventDAO.record(user.getUserId(), "REPORT_EXPORT", "OptimizationReportSnapshot", "SUCCESS");
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Saved report could not be generated.");
        }
    }
}
