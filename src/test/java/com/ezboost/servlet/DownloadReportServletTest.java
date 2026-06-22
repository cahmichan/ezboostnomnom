package com.ezboost.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DownloadReportServletTest {

    @Test
    void rejectsGetRequestsBecauseReportExportsAreMutations() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        new DownloadReportServlet().doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "Use POST to export reports.");
    }
}
