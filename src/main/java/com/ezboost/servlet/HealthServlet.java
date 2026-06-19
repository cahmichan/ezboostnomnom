package com.ezboost.servlet;

import com.ezboost.util.DBConnection;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;

/** Minimal, non-sensitive readiness endpoint for server monitoring. */
public class HealthServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        try (Connection connection = DBConnection.getConnection()) {
            if (!connection.isValid(2)) {
                throw new java.sql.SQLException("Database connection is not valid");
            }
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"status\":\"ok\"}");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.getWriter().write("{\"status\":\"unavailable\"}");
        }
    }
}
