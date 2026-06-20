package com.ezboost.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InputSafetyFilterPasswordTest {

    @Test
    void permitsPasswordsContainingSpecialCharacters() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getParameterNames()).thenReturn(Collections.enumeration(Collections.singletonList("password")));
        when(request.getParameterValues("password")).thenReturn(new String[]{"a<'b>\"c"});

        new InputSafetyFilter().doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
