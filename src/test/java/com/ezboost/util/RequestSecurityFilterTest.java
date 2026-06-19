package com.ezboost.util;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RequestSecurityFilterTest {

    @Test
    void reusesTheSameSessionCsrfToken() {
        HttpSession session = Mockito.mock(HttpSession.class);
        java.util.Map<String, Object> attributes = new java.util.HashMap<>();
        Mockito.when(session.getAttribute(RequestSecurityFilter.CSRF_SESSION_KEY))
                .thenAnswer(invocation -> attributes.get(invocation.getArgument(0)));
        Mockito.doAnswer(invocation -> {
            attributes.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(session).setAttribute(Mockito.anyString(), Mockito.any());

        String first = RequestSecurityFilter.getOrCreateToken(session);
        String second = RequestSecurityFilter.getOrCreateToken(session);

        assertNotNull(first);
        assertEquals(first, second);
    }

    @Test
    void rejectsAuthenticatedPostWithoutCsrfToken() throws Exception {
        HttpSession session = Mockito.mock(HttpSession.class);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);
        Mockito.when(request.getMethod()).thenReturn("POST");
        Mockito.when(request.getContextPath()).thenReturn("");
        Mockito.when(request.getRequestURI()).thenReturn("/DataImport");
        Mockito.when(request.getSession(false)).thenReturn(session);
        Mockito.when(session.getAttribute(RequestSecurityFilter.CSRF_SESSION_KEY)).thenReturn("valid-token");
        Mockito.when(request.getParameter(RequestSecurityFilter.CSRF_PARAMETER)).thenReturn(null);
        Mockito.when(request.getHeader("X-CSRF-Token")).thenReturn(null);

        new RequestSecurityFilter().doFilter(request, response, chain);

        verify(response).sendError(Mockito.eq(HttpServletResponse.SC_FORBIDDEN), Mockito.anyString());
        verify(chain, never()).doFilter(Mockito.any(), Mockito.any());
    }
}
