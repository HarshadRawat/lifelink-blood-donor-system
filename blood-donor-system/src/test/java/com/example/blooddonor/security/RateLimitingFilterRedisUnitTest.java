package com.example.blooddonor.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterRedisUnitTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private FilterChain filterChain;

    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        rateLimitingFilter = new RateLimitingFilter(true, 5, 60, Set.of("127.0.0.1"), redisTemplate);
    }

    @Test
    @DisplayName("Redis INCR initializes key and sets 60-second TTL on first request")
    void testRedisSetsTtlOnFirstRequest() throws ServletException, IOException {
        String clientIp = "192.168.1.50";
        String expectedKey = "rl:login:" + clientIp;

        when(valueOperations.increment(expectedKey)).thenReturn(1L);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setServletPath("/api/auth/login");
        request.setRemoteAddr(clientIp);
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitingFilter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        verify(valueOperations).increment(expectedKey);
        verify(redisTemplate).expire(expectedKey, Duration.ofSeconds(60));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Subsequent requests increment counter but do not reset TTL")
    void testSubsequentRequestsDoNotResetTtl() throws ServletException, IOException {
        String clientIp = "192.168.1.51";
        String expectedKey = "rl:login:" + clientIp;

        when(valueOperations.increment(expectedKey)).thenReturn(2L);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setServletPath("/api/auth/login");
        request.setRemoteAddr(clientIp);
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitingFilter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        verify(valueOperations).increment(expectedKey);
        verify(redisTemplate, never()).expire(eq(expectedKey), any(Duration.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Request exceeding quota via Redis counter returns HTTP 429 Too Many Requests")
    void testRedisRateLimitEnforcedOnQuotaExceeded() throws ServletException, IOException {
        String clientIp = "192.168.1.52";
        String expectedKey = "rl:login:" + clientIp;

        when(valueOperations.increment(expectedKey)).thenReturn(6L); // Exceeds maxRequests = 5

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setServletPath("/api/auth/login");
        request.setRemoteAddr(clientIp);
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitingFilter.doFilter(request, response, filterChain);

        assertEquals(429, response.getStatus());
        assertEquals("60", response.getHeader("Retry-After"));
        assertTrue(response.getContentAsString().contains("Too many authentication attempts"));
        assertTrue(response.getContentAsString().contains("Too Many Requests"));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Redis outage gracefully falls back to local rate limiting without throwing HTTP 500")
    void testRedisOutageFallsBackToLocalWithout500() throws ServletException, IOException {
        String clientIp = "192.168.1.53";
        String expectedKey = "rl:login:" + clientIp;

        when(valueOperations.increment(expectedKey))
                .thenThrow(new RedisConnectionFailureException("Unable to connect to Redis on localhost:6379"));

        // First 5 requests should pass via local in-memory fallback
        for (int i = 1; i <= 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.setServletPath("/api/auth/login");
            request.setRemoteAddr(clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitingFilter.doFilter(request, response, filterChain);
            assertEquals(200, response.getStatus(), "Fallback must allow requests up to local limit");
        }

        // 6th request must be rate limited by the local in-memory tracker (HTTP 429)
        MockHttpServletRequest request6 = new MockHttpServletRequest("POST", "/api/auth/login");
        request6.setServletPath("/api/auth/login");
        request6.setRemoteAddr(clientIp);
        MockHttpServletResponse response6 = new MockHttpServletResponse();

        rateLimitingFilter.doFilter(request6, response6, filterChain);
        assertEquals(429, response6.getStatus(), "Local tracker must still enforce quota during Redis outage");
        verify(filterChain, times(5)).doFilter(any(), any());
    }

    @Test
    @DisplayName("Reset method clears keys matching pattern in Redis")
    void testResetClearsRedisKeys() {
        when(redisTemplate.keys("rl:login:*")).thenReturn(Set.of("rl:login:10.0.0.1", "rl:login:10.0.0.2"));

        rateLimitingFilter.reset();

        verify(redisTemplate).keys("rl:login:*");
        verify(redisTemplate).delete(Set.of("rl:login:10.0.0.1", "rl:login:10.0.0.2"));
    }
}
