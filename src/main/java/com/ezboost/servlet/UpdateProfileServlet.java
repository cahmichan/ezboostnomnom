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
import java.util.LinkedHashMap;
import java.util.Map;

public class UpdateProfileServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        User loggedUser = (User) session.getAttribute("user");

        String firstName = UserValidationUtil.normalize(request.getParameter("firstName"));
        String lastName = UserValidationUtil.normalize(request.getParameter("lastName"));
        String username = UserValidationUtil.normalize(request.getParameter("username"));
        String email = UserValidationUtil.normalize(request.getParameter("email"));
        String phoneNumber = UserValidationUtil.normalize(request.getParameter("phoneNumber"));
        String newPassword = request.getParameter("newPassword");

        Map<String, String> formData = new LinkedHashMap<>();
        formData.put("firstName", firstName);
        formData.put("lastName", lastName);
        formData.put("username", username);
        formData.put("email", email);
        formData.put("phoneNumber", phoneNumber);

        String validationError = UserValidationUtil.validateProfile(
                firstName, lastName, username, email, newPassword, phoneNumber, false);
        if (validationError != null) {
            fail(session, formData, validationError, response);
            return;
        }

        if (UserDAO.isUsernameTaken(username, loggedUser.getUserId())) {
            fail(session, formData, "That username is already taken by another account.", response);
            return;
        }

        if (UserDAO.isEmailTaken(email, loggedUser.getUserId())) {
            fail(session, formData, "That email is already being used by another account.", response);
            return;
        }

        loggedUser.setFirstName(firstName);
        loggedUser.setLastName(lastName);
        loggedUser.setUsername(username);
        loggedUser.setEmail(email);
        loggedUser.setPhoneNumber(phoneNumber);

        if (!UserValidationUtil.isEmpty(newPassword)) {
            loggedUser.setPassword(newPassword);
        }

        boolean success = UserDAO.updateUser(loggedUser);

        if (success) {
            session.setAttribute("user", UserDAO.getUserById(loggedUser.getUserId()));
            session.setAttribute("profileSuccess", "Profile updated successfully.");
            session.removeAttribute("profileFormData");
        } else {
            session.setAttribute("profileError", "Profile update failed. Please try again.");
            session.setAttribute("profileFormData", formData);
        }

        response.sendRedirect("Profile");
    }

    private void fail(HttpSession session, Map<String, String> formData, String message,
                      HttpServletResponse response) throws IOException {
        session.setAttribute("profileError", message);
        session.setAttribute("profileFormData", formData);
        response.sendRedirect("Profile?edit=1");
    }
}
