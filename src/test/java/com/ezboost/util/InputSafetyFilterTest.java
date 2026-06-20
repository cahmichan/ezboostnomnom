package com.ezboost.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Enumeration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InputSafetyFilterTest {

    @Test
    void rejectsMarkupDelimitersInPostFormData() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getParameterNames()).thenReturn(enumeration("eventName"));
        when(request.getParameterValues("eventName")).thenReturn(new String[]{"<script>"});

        new InputSafetyFilter().doFilter(request, response, chain);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST,
                "The submitted form contains unsupported characters.");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void permitsNormalPostFormData() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getParameterNames()).thenReturn(enumeration("eventName"));
        when(request.getParameterValues("eventName")).thenReturn(new String[]{"Hotel Anniversary"});

        new InputSafetyFilter().doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    private Enumeration<String> enumeration(String value) {
        return Collections.enumeration(Collections.singletonList(value));
    }
}
