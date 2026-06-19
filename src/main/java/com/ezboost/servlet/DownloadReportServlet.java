package com.ezboost.servlet;

import com.ezboost.dao.MarketSegmentDAO;
import com.ezboost.model.MarketSegment;
import com.ezboost.model.Room;
import com.ezboost.model.User;
import com.ezboost.service.ExcelExportService;
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
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No session found.");
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
            User user = (User) session.getAttribute("user");
            int userId = user != null ? user.getUserId() : 0;
            marketSegments = MarketSegmentDAO.getAllSegments(userId);
            if (marketSegments == null || marketSegments.isEmpty()) {
                MarketSegmentDAO.initializeDefaultSegments(userId);
                marketSegments = MarketSegmentDAO.getAllSegments(userId);
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
    }
}
