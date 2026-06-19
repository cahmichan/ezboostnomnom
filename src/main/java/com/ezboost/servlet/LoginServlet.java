package com.ezboost.servlet;

import com.ezboost.dao.UserDAO;
import com.ezboost.model.User;
import com.ezboost.util.UserValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

public class LoginServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = UserValidationUtil.normalize(request.getParameter("email"));
        String password = request.getParameter("password");

        if (UserValidationUtil.isEmpty(email) || UserValidationUtil.isEmpty(password)) {
            request.setAttribute("error", "Please enter both email and password.");
            request.getRequestDispatcher("login.jsp").forward(request, response);
            return;
        }

        User user = UserDAO.loginUser(email, password);

        if (user != null) {
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            HttpSession session = request.getSession(true);

            session.setAttribute("user", user);
            session.setAttribute("userId", user.getUserId());

            response.sendRedirect("homepage.jsp");
        } else {
            request.setAttribute("error", "Wrong email or password. Please try again.");
            request.setAttribute("email", email);
            request.getRequestDispatcher("login.jsp").forward(request, response);
        }
    }
}
