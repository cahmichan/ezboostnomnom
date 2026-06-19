package com.ezboost.util;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
}
