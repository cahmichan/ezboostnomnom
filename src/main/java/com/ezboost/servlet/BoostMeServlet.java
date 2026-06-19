package com.ezboost.servlet;

import com.ezboost.model.User;
import com.ezboost.service.BoostMePageService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Serves the optimization workspace with server-prepared setup status. */
public class BoostMeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (request.getSession(false) == null) {
            response.sendRedirect("login.jsp");
            return;
        }
        User user = (User) request.getSession(false).getAttribute("user");
        BoostMePageService.prepare(request, user);
        request.getRequestDispatcher("BoostMe.jsp").forward(request, response);
    }
}
