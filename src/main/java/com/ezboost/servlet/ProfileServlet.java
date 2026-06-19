package com.ezboost.servlet;

import com.ezboost.dao.OptimizationRequestDAO;
import com.ezboost.dao.OptimizationResultDAO;
import com.ezboost.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

public class ProfileServlet extends HttpServlet {

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

        long daysActive;
        if (user.getCreatedAt() != null) {
            long diffMs = System.currentTimeMillis() - user.getCreatedAt().getTime();
            daysActive = Math.max(1, diffMs / (1000 * 60 * 60 * 24));
        } else {
            daysActive = 1;
        }

        int optimizationCount = OptimizationRequestDAO.getOptimizationCount(userId);
        double bestRevenue = OptimizationResultDAO.getBestTotalRevenue(userId);
        Timestamp lastRunDate = OptimizationRequestDAO.getLastOptimizationDate(userId);
        List<Map<String, Object>> recentOptimizations = OptimizationRequestDAO.getRecentOptimizations(userId, 5);

        DecimalFormat moneyFormat = new DecimalFormat("#,##0.00");
        String bestRevenueFormatted = moneyFormat.format(bestRevenue);

        request.setAttribute("daysActive", daysActive);
        request.setAttribute("optimizationCount", optimizationCount);
        request.setAttribute("bestRevenue", bestRevenueFormatted);
        request.setAttribute("bestRevenueRaw", bestRevenue);
        request.setAttribute("lastRunDate", lastRunDate);
        request.setAttribute("recentOptimizations", recentOptimizations);
        request.setAttribute("profileError", session.getAttribute("profileError"));
        request.setAttribute("profileSuccess", session.getAttribute("profileSuccess"));
        request.setAttribute("profileFormData", session.getAttribute("profileFormData"));
        request.setAttribute("openEditModal", "1".equals(request.getParameter("edit")));

        session.removeAttribute("profileError");
        session.removeAttribute("profileSuccess");
        session.removeAttribute("profileFormData");

        request.getRequestDispatcher("/profile.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}
