package com.ezboost.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NavigationViewFilterTest {

    @Test
    void preparesAnonymousNavigationWithoutDatabaseAccess() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getSession(false)).thenReturn(null);

        new NavigationViewFilter().doFilter(request, response, chain);

        verify(request).setAttribute("navHomeLink", "homepage.jsp");
        verify(request).setAttribute("navBoostLink", "DataImport");
        verify(chain).doFilter(request, response);
    }
}
