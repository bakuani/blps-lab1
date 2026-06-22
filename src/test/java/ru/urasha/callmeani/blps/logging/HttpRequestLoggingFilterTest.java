package ru.urasha.callmeani.blps.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HttpRequestLoggingFilterTest {

    private final HttpRequestLoggingFilter filter = new HttpRequestLoggingFilter();

    @Test
    void preservesSafeCorrelationIdAndReturnsItToClient() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/tariffs");
        request.addHeader(HttpRequestLoggingFilter.CORRELATION_HEADER, "demo-correlation-42");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) ->
            ((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_ACCEPTED);

        filter.doFilter(request, response, chain);

        assertEquals(HttpServletResponse.SC_ACCEPTED, response.getStatus());
        assertEquals(
            "demo-correlation-42",
            response.getHeader(HttpRequestLoggingFilter.CORRELATION_HEADER)
        );
    }

    @Test
    void replacesUnsafeCorrelationIdWithUuid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/tariffs");
        request.addHeader(HttpRequestLoggingFilter.CORRELATION_HEADER, "unsafe value with spaces");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
        });

        String correlationId = response.getHeader(HttpRequestLoggingFilter.CORRELATION_HEADER);
        assertNotNull(correlationId);
        UUID.fromString(correlationId);
    }
}
