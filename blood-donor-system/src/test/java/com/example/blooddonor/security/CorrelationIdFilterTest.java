package com.example.blooddonor.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        MDC.clear();
    }

    @Test
    @DisplayName("Filter should generate UUID, populate MDC, and set response header when incoming header is missing")
    void doFilter_withoutHeader_shouldGenerateCorrelationId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> capturedMdc = new AtomicReference<>();
        FilterChain chain = (req, res) -> capturedMdc.set(MDC.get(CorrelationIdFilter.MDC_KEY));

        filter.doFilter(request, response, chain);

        String generatedId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertNotNull(generatedId, "Response should have X-Correlation-ID header");
        assertFalse(generatedId.isBlank(), "Generated ID should not be blank");
        assertEquals(generatedId, capturedMdc.get(), "MDC should contain generated correlation ID during execution");
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY), "MDC should be cleared after request finishes");
    }

    @Test
    @DisplayName("Filter should preserve existing X-Correlation-ID header from client")
    void doFilter_withExistingCorrelationIdHeader_shouldPreserveHeader() throws ServletException, IOException {
        String existingId = "client-trace-12345";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, existingId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> capturedMdc = new AtomicReference<>();
        FilterChain chain = (req, res) -> capturedMdc.set(MDC.get(CorrelationIdFilter.MDC_KEY));

        filter.doFilter(request, response, chain);

        assertEquals(existingId, response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER));
        assertEquals(existingId, capturedMdc.get());
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @Test
    @DisplayName("Filter should fallback to X-Request-ID when X-Correlation-ID is absent")
    void doFilter_withRequestIdHeader_shouldUseRequestId() throws ServletException, IOException {
        String requestId = "req-abc-987";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.REQUEST_ID_HEADER, requestId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> capturedMdc = new AtomicReference<>();
        FilterChain chain = (req, res) -> capturedMdc.set(MDC.get(CorrelationIdFilter.MDC_KEY));

        filter.doFilter(request, response, chain);

        assertEquals(requestId, response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER));
        assertEquals(requestId, capturedMdc.get());
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }
}
