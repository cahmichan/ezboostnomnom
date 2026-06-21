package com.ezboost.util;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Enumeration;

/** Rejects control characters and markup delimiters in normal form fields. */
public class InputSafetyFilter implements Filter {

    @Override
    public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        if ("POST".equalsIgnoreCase(httpRequest.getMethod()) && !isMultipart(httpRequest)
                && containsUnsafeFormValue(httpRequest)) {
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "The submitted form contains unsupported characters.");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase(java.util.Locale.ROOT).startsWith("multipart/");
    }

    private boolean containsUnsafeFormValue(HttpServletRequest request) {
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (containsUnsafeCharacters(name)) {
                return true;
            }
            String[] values = request.getParameterValues(name);
            if (values == null) continue;
            for (String value : values) {
                if (!isSecretField(name) && containsUnsafeCharacters(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSecretField(String name) {
        return "password".equals(name) || "newPassword".equals(name);
    }

    private boolean containsUnsafeCharacters(String value) {
        if (value == null) return false;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current < 0x20 || current == '<' || current == '>' || current == '\'' || current == '"') {
                return true;
            }
        }
        return false;
    }
}
