package com.ezboost.util;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import org.slf4j.MDC;

/** Applies response hardening and validates CSRF tokens for authenticated mutations. */
public class RequestSecurityFilter implements Filter {

    public static final String CSRF_SESSION_KEY = "csrfToken";
    public static final String CSRF_PARAMETER = "csrfToken";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public void init(FilterConfig filterConfig) {
        // Servlet filter lifecycle hook.
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestId = UUID.randomUUID().toString();
        httpResponse.setHeader("X-Request-Id", requestId);
        MDC.put("requestId", requestId);
        try {
            applyHeaders(httpResponse);
            String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());

            if (requiresCsrfValidation(httpRequest, path) && !hasValidCsrfToken(httpRequest)) {
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Your form session has expired. Refresh the page and try again.");
                return;
            }

            HttpSession session = httpRequest.getSession(false);
            if (session != null && session.getAttribute("user") != null) {
                request.setAttribute(CSRF_PARAMETER, getOrCreateToken(session));
            }
            chain.doFilter(request, response);
        } finally {
            MDC.remove("requestId");
        }
    }

    public static String getOrCreateToken(HttpSession session) {
        Object existing = session.getAttribute(CSRF_SESSION_KEY);
        if (existing instanceof String && !((String) existing).isEmpty()) {
            return (String) existing;
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(CSRF_SESSION_KEY, token);
        return token;
    }

    private boolean requiresCsrfValidation(HttpServletRequest request, String path) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        return !"/LoginServlet".equals(path) && !"/RegisterServlet".equals(path);
    }

    private boolean hasValidCsrfToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        Object expected = session.getAttribute(CSRF_SESSION_KEY);
        String supplied = request.getParameter(CSRF_PARAMETER);
        if (supplied == null || supplied.isEmpty()) {
            supplied = request.getHeader("X-CSRF-Token");
        }
        return expected instanceof String && constantTimeEquals((String) expected, supplied);
    }

    private boolean constantTimeEquals(String expected, String supplied) {
        if (supplied == null) {
            return false;
        }
        return java.security.MessageDigest.isEqual(
                expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                supplied.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private void applyHeaders(HttpServletResponse response) {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "same-origin");
        response.setHeader("Content-Security-Policy",
                "default-src 'self'; img-src 'self' data: https:; style-src 'self' 'unsafe-inline' https:; "
                        + "script-src 'self' 'unsafe-inline' https:; font-src 'self' https:; connect-src 'self'");
    }
}
