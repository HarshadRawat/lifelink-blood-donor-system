package com.example.blooddonor.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Multi-instance proof and integration test for distributed rate limiting via Redis.
 * Verifies that multiple independent backend instances sharing Redis enforce a unified quota.
 */
@Testcontainers(disabledWithoutDocker = true)
class RedisRateLimitingIntegrationTest {

    @Container
    private static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redisTemplate;

    private RateLimitingFilter node1;
    private RateLimitingFilter node2;

    @BeforeEach
    void setUp() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(
                redis.getHost(),
                redis.getMappedPort(6379)
        );
        connectionFactory = new LettuceConnectionFactory(config);
        connectionFactory.afterPropertiesSet();

        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();

        // Ensure clean state before each test
        Set<String> existingKeys = redisTemplate.keys("rl:login:*");
        if (existingKeys != null && !existingKeys.isEmpty()) {
            redisTemplate.delete(existingKeys);
        }

        // Two distinct filter instances representing two scaled backend server nodes
        // Max 10 requests per 60-second window across the entire cluster
        node1 = new RateLimitingFilter(true, 10, 60, Set.of("127.0.0.1"), redisTemplate);
        node2 = new RateLimitingFilter(true, 10, 60, Set.of("127.0.0.1"), redisTemplate);
    }

    @AfterEach
    void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    @DisplayName("Multi-instance rate limiting: 6 requests to Node 1 + 4 to Node 2 succeed, 11th on Node 2 is blocked")
    void testDistributedRateLimitingEnforcedAcrossNodes() throws Exception {
        FilterChain filterChain = mock(FilterChain.class);
        String clientIp = "203.0.113.88";

        // Step 1: Send 6 requests to Node 1
        for (int i = 1; i <= 6; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setServletPath("/api/auth/login");
            req.setRemoteAddr(clientIp);
            MockHttpServletResponse res = new MockHttpServletResponse();

            node1.doFilter(req, res, filterChain);
            assertEquals(200, res.getStatus(), "Node 1 request #" + i + " must succeed");
        }

        // Step 2: Send 4 requests to Node 2 (sharing the same Redis state)
        for (int i = 7; i <= 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setServletPath("/api/auth/login");
            req.setRemoteAddr(clientIp);
            MockHttpServletResponse res = new MockHttpServletResponse();

            node2.doFilter(req, res, filterChain);
            assertEquals(200, res.getStatus(), "Node 2 request #" + i + " must succeed");
        }

        // Step 3: Total is now 10 requests across both nodes. 11th request on Node 2 must be blocked!
        MockHttpServletRequest blockedReq = new MockHttpServletRequest("POST", "/api/auth/login");
        blockedReq.setServletPath("/api/auth/login");
        blockedReq.setRemoteAddr(clientIp);
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();

        node2.doFilter(blockedReq, blockedRes, filterChain);

        assertEquals(429, blockedRes.getStatus(), "11th request across cluster nodes must be rejected with 429");
        assertEquals("60", blockedRes.getHeader("Retry-After"));
        assertTrue(blockedRes.getContentAsString().contains("Too many authentication attempts"));
        assertTrue(blockedRes.getContentAsString().contains("429"));

        // Filter chain should have been invoked exactly 10 times in total
        verify(filterChain, times(10)).doFilter(any(), any());
    }

    @Test
    @DisplayName("Redis key has correct TTL set on creation")
    void testRedisKeyHasExpirationTtl() throws Exception {
        FilterChain filterChain = mock(FilterChain.class);
        String clientIp = "198.51.100.99";

        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
        req.setServletPath("/api/auth/login");
        req.setRemoteAddr(clientIp);
        MockHttpServletResponse res = new MockHttpServletResponse();

        node1.doFilter(req, res, filterChain);
        assertEquals(200, res.getStatus());

        String key = "rl:login:" + clientIp;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

        assertNotNull(ttl, "TTL must not be null");
        assertTrue(ttl > 0 && ttl <= 60, "TTL must be between 1 and 60 seconds, but was: " + ttl);
    }

    @Test
    @DisplayName("Reset method cleans all rate limit keys from Redis")
    void testResetClearsRedisState() throws Exception {
        FilterChain filterChain = mock(FilterChain.class);

        MockHttpServletRequest req1 = new MockHttpServletRequest("POST", "/api/auth/login");
        req1.setServletPath("/api/auth/login");
        req1.setRemoteAddr("10.0.0.1");
        node1.doFilter(req1, new MockHttpServletResponse(), filterChain);

        MockHttpServletRequest req2 = new MockHttpServletRequest("POST", "/api/auth/login");
        req2.setServletPath("/api/auth/login");
        req2.setRemoteAddr("10.0.0.2");
        node2.doFilter(req2, new MockHttpServletResponse(), filterChain);

        assertEquals("1", redisTemplate.opsForValue().get("rl:login:10.0.0.1"));
        assertEquals("1", redisTemplate.opsForValue().get("rl:login:10.0.0.2"));

        node1.reset();

        assertNull(redisTemplate.opsForValue().get("rl:login:10.0.0.1"));
        assertNull(redisTemplate.opsForValue().get("rl:login:10.0.0.2"));
    }

    @Test
    @DisplayName("Unreachable Redis instance gracefully fails open to local rate limiting")
    void testUnreachableRedisFallsBackGracefully() throws Exception {
        FilterChain filterChain = mock(FilterChain.class);

        // Point to an invalid port where Redis is definitely not listening
        RedisStandaloneConfiguration unreachableConfig = new RedisStandaloneConfiguration("127.0.0.1", 65432);
        LettuceConnectionFactory brokenFactory = new LettuceConnectionFactory(unreachableConfig);
        brokenFactory.afterPropertiesSet();
        StringRedisTemplate brokenTemplate = new StringRedisTemplate(brokenFactory);
        brokenTemplate.afterPropertiesSet();

        RateLimitingFilter resilientNode = new RateLimitingFilter(true, 5, 60, Set.of("127.0.0.1"), brokenTemplate);

        try {
            // First 5 requests should still be handled via local fallback without 500 error
            for (int i = 1; i <= 5; i++) {
                MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
                req.setServletPath("/api/auth/login");
                req.setRemoteAddr("172.16.0.5");
                MockHttpServletResponse res = new MockHttpServletResponse();

                resilientNode.doFilter(req, res, filterChain);
                assertEquals(200, res.getStatus(), "Fallback must allow legitimate requests");
            }

            // 6th request is throttled locally
            MockHttpServletRequest blockedReq = new MockHttpServletRequest("POST", "/api/auth/login");
            blockedReq.setServletPath("/api/auth/login");
            blockedReq.setRemoteAddr("172.16.0.5");
            MockHttpServletResponse blockedRes = new MockHttpServletResponse();

            resilientNode.doFilter(blockedReq, blockedRes, filterChain);
            assertEquals(429, blockedRes.getStatus(), "Local tracker must reject 6th request");
        } finally {
            brokenFactory.destroy();
        }
    }
}
