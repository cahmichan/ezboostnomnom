package com.ezboost.servlet;

import com.ezboost.dao.UserDAO;
import com.ezboost.model.User;
import com.ezboost.util.UserValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RegisterServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(RegisterServlet.class);

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        logger.debug("RegisterServlet called");

        String firstName = UserValidationUtil.normalize(request.getParameter("firstName"));
        String lastName = UserValidationUtil.normalize(request.getParameter("lastName"));
        String username = UserValidationUtil.normalize(request.getParameter("username"));
        String email = UserValidationUtil.normalize(request.getParameter("email"));
        String password = request.getParameter("password");
        String phoneNumber = UserValidationUtil.normalize(request.getParameter("phoneNumber"));

        request.setAttribute("firstName", firstName);
        request.setAttribute("lastName", lastName);
        request.setAttribute("username", username);
        request.setAttribute("email", email);
        request.setAttribute("phoneNumber", phoneNumber);

        String validationError = UserValidationUtil.validateProfile(
                firstName, lastName, username, email, password, phoneNumber, true);
        if (validationError != null) {
            forwardError(request, response, validationError);
            return;
        }

        if (UserDAO.isUsernameTaken(username)) {
            forwardError(request, response,
                    "Username \"" + username + "\" is already taken. Please choose a different one.");
            return;
        }

        if (UserDAO.isEmailTaken(email)) {
            forwardError(request, response,
                    "An account with this email already exists. Please use a different email or login instead.");
            return;
        }

        User newUser = new User(firstName, lastName, username, email, password, phoneNumber);
        boolean isRegistered = UserDAO.registerUser(newUser);

        logger.debug("Database insert status: {}", isRegistered);

        if (isRegistered) {
            response.sendRedirect("login.jsp?registered=1");
        } else {
            forwardError(request, response, "Registration failed due to a server error. Please try again.");
        }
    }

    private void forwardError(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        request.setAttribute("error", message);
        request.getRequestDispatcher("register.jsp").forward(request, response);
    }
}
