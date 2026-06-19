package com.ezboost.util;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import com.ezboost.model.User;
import java.io.IOException;

/**
 * Authentication filter that checks for a valid user session.
 * Protects all servlets except login, register, logout, and static resources.
 */
public class AuthenticationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = uri.substring(contextPath.length());

        // Allow access to public resources without authentication
        if (isPublicResource(path)) {
            chain.doFilter(req, res);
            return;
        }

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(contextPath + "/login.jsp");
            return;
        }

        User user = (User) session.getAttribute("user");
        if (!OnboardingUtil.isAllowedDuringOnboarding(path, user)) {
            response.sendRedirect(contextPath + "/" + OnboardingUtil.getStepUrl(OnboardingUtil.getCurrentStep(user)));
            return;
        }

        chain.doFilter(req, res);
    }

    private boolean isPublicResource(String path) {
        return path.equals("/login.jsp")
            || path.equals("/register.jsp")
            || path.equals("/LoginServlet")
            || path.equals("/RegisterServlet")
            || path.equals("/LogoutServlet")
            || path.startsWith("/css/")
            || path.startsWith("/js/")
            || path.startsWith("/images/");
    }
}
