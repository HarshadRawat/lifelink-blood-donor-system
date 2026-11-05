package com.example.blooddonor.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitingFilterTest {

    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUp() {
        // 5 requests per 60-second window for testing
        rateLimitingFilter = new RateLimitingFilter(true, 5, 60);
    }

    @Test
    @DisplayName("Requests within the limit are allowed through to the filter chain")
    void requestsUnderLimitAreAllowed() throws ServletException, IOException {
        FilterChain filterChain = mock(FilterChain.class);

        for (int i = 1; i <= 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.setServletPath("/api/auth/login");
            request.setRemoteAddr("192.168.1.100");
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitingFilter.doFilter(request, response, filterChain);

            assertEquals(200, response.getStatus());
        }

        // Verify the filter chain was invoked exactly 5 times
        verify(filterChain, times(5)).doFilter(any(), any());
    }

    @Test
    @DisplayName("Requests exceeding the threshold return HTTP 429 Too Many Requests with Retry-After header")
    void requestsExceedingLimitAreBlocked() throws ServletException, IOException {
        FilterChain filterChain = mock(FilterChain.class);

        // First 5 requests allowed
        for (int i = 1; i <= 5; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setServletPath("/api/auth/login");
            req.setRemoteAddr("10.0.0.1");
            MockHttpServletResponse res = new MockHttpServletResponse();
            rateLimitingFilter.doFilter(req, res, filterChain);
            assertEquals(200, res.getStatus());
        }

        // 6th request must be rejected
        MockHttpServletRequest blockedReq = new MockHttpServletRequest("POST", "/api/auth/login");
        blockedReq.setServletPath("/api/auth/login");
        blockedReq.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();

        rateLimitingFilter.doFilter(blockedReq, blockedRes, filterChain);

        assertEquals(429, blockedRes.getStatus());
        assertEquals("60", blockedRes.getHeader("Retry-After"));
        assertTrue(blockedRes.getContentAsString().contains("Too Many Requests"));

        // Filter chain should NOT have been called on 6th request (still 5 total)
        verify(filterChain, times(5)).doFilter(any(), any());
    }

    @Test
    @DisplayName("Different client IP addresses have independent rate limit buckets")
    void differentIpsHaveIndependentCounters() throws ServletException, IOException {
        FilterChain filterChain = mock(FilterChain.class);

        // Exhaust IP 10.0.0.1 limit
        for (int i = 1; i <= 5; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setServletPath("/api/auth/login");
            req.setRemoteAddr("10.0.0.1");
            MockHttpServletResponse res = new MockHttpServletResponse();
            rateLimitingFilter.doFilter(req, res, filterChain);
        }

        // IP 10.0.0.2 should still be allowed
        MockHttpServletRequest otherReq = new MockHttpServletRequest("POST", "/api/auth/login");
        otherReq.setServletPath("/api/auth/login");
        otherReq.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse otherRes = new MockHttpServletResponse();

        rateLimitingFilter.doFilter(otherReq, otherRes, filterChain);

        assertEquals(200, otherRes.getStatus());
    }

    @Test
    @DisplayName("Non-authentication endpoints are not subject to rate limiting")
    void nonTargetEndpointsAreNotRateLimited() throws ServletException, IOException {
        FilterChain filterChain = mock(FilterChain.class);

        // Make 10 requests to a non-auth endpoint
        for (int i = 1; i <= 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/donors/profile");
            req.setServletPath("/api/donors/profile");
            req.setRemoteAddr("10.0.0.5");
            MockHttpServletResponse res = new MockHttpServletResponse();

            rateLimitingFilter.doFilter(req, res, filterChain);
            assertEquals(200, res.getStatus());
        }

        verify(filterChain, times(10)).doFilter(any(), any());
    }

    @Test
    @DisplayName("When rate limiting is disabled, requests exceeding threshold are not blocked")
    void disabledFilterDoesNotBlock() throws ServletException, IOException {
        RateLimitingFilter disabledFilter = new RateLimitingFilter(false, 3, 60);
        FilterChain filterChain = mock(FilterChain.class);

        for (int i = 1; i <= 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setServletPath("/api/auth/login");
            req.setRemoteAddr("10.0.0.9");
            MockHttpServletResponse res = new MockHttpServletResponse();

            disabledFilter.doFilter(req, res, filterChain);
            assertEquals(200, res.getStatus());
        }

        verify(filterChain, times(10)).doFilter(any(), any());
    }

    @Test
    @DisplayName("X-Forwarded-For header takes precedence over remoteAddr for reverse proxy support")
    void xForwardedForHeaderIsUsed() throws ServletException, IOException {
        FilterChain filterChain = mock(FilterChain.class);

        // Exhaust limit using X-Forwarded-For header
        for (int i = 1; i <= 5; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setServletPath("/api/auth/login");
            req.setRemoteAddr("127.0.0.1"); // Nginx proxy address
            req.addHeader("X-Forwarded-For", "203.0.113.195, 10.0.0.1"); // Real client IP
            MockHttpServletResponse res = new MockHttpServletResponse();

            rateLimitingFilter.doFilter(req, res, filterChain);
        }

        // 6th request with same X-Forwarded-For should be blocked
        MockHttpServletRequest blockedReq = new MockHttpServletRequest("POST", "/api/auth/login");
        blockedReq.setServletPath("/api/auth/login");
        blockedReq.setRemoteAddr("127.0.0.1");
        blockedReq.addHeader("X-Forwarded-For", "203.0.113.195");
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();

        rateLimitingFilter.doFilter(blockedReq, blockedRes, filterChain);

        assertEquals(429, blockedRes.getStatus());
    }
}

