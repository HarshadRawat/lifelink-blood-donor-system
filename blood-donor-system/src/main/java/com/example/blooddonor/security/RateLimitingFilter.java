package com.example.blooddonor.security;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe distributed rate limiting filter protecting authentication endpoints
 * against brute-force and credential stuffing attacks.
 * Uses Redis atomic counters with TTL across multi-instance deployments,
 * with graceful fallback to local in-memory sliding-window if Redis is unavailable.
 * Resists X-Forwarded-For spoofing by honoring headers only from trusted reverse proxies.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    @Value("${security.rate-limit.enabled:true}")
    private boolean enabled = true;

    @Value("${security.rate-limit.login.max-requests:10}")
    private int maxRequests = 10;

    @Value("${security.rate-limit.login.window-seconds:60}")
    private long windowSeconds = 60;

    @Value("${security.rate-limit.trusted-proxies:127.0.0.1,0:0:0:0:0:0:0:1,::1}")
    private String trustedProxiesConfig = "127.0.0.1,0:0:0:0:0:0:0:1,::1";

    private final Set<String> trustedProxies = new HashSet<>(Set.of("127.0.0.1", "0:0:0:0:0:0:0:1", "::1"));

    private final Map<String, WindowTracker> ipTrackers = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    public RateLimitingFilter() {
    }

    public RateLimitingFilter(boolean enabled, int maxRequests, long windowSeconds) {
        this(enabled, maxRequests, windowSeconds, Set.of("127.0.0.1", "0:0:0:0:0:0:0:1", "::1"), null);
    }

    public RateLimitingFilter(boolean enabled, int maxRequests, long windowSeconds, Set<String> trustedProxies) {
        this(enabled, maxRequests, windowSeconds, trustedProxies, null);
    }

    public RateLimitingFilter(boolean enabled, int maxRequests, long windowSeconds, Set<String> trustedProxies, StringRedisTemplate redisTemplate) {
        this.enabled = enabled;
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
        this.trustedProxies.clear();
        if (trustedProxies != null) {
            this.trustedProxies.addAll(trustedProxies);
        }
        this.redisTemplate = redisTemplate;
    }

    @Autowired(required = false)
    public void setRedisTemplate(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public StringRedisTemplate getRedisTemplate() {
        return redisTemplate;
    }

    @PostConstruct
    void initTrustedProxies() {
        if (trustedProxiesConfig != null && !trustedProxiesConfig.isBlank()) {
            trustedProxies.clear();
            Arrays.stream(trustedProxiesConfig.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(trustedProxies::add);
        }
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getServletPath();
        String method = request.getMethod();

        // Protect login and registration from brute force attacks
        if ("POST".equalsIgnoreCase(method) && ("/api/auth/login".equals(path) || "/api/auth/register".equals(path))) {
            String clientIp = getClientIp(request);
            boolean allowed = checkRateLimit(clientIp);

            if (!allowed) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setHeader("Retry-After", String.valueOf(windowSeconds));
                response.getWriter().write(String.format(
                        "{\"error\":\"Too Many Requests\",\"message\":\"Too many authentication attempts. Please try again after %d seconds.\",\"status\":429}",
                        windowSeconds
                ));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    boolean checkRateLimit(String clientIp) {
        if (redisTemplate != null) {
            try {
                String key = "rl:login:" + clientIp;
                Long count = redisTemplate.opsForValue().increment(key);
                if (count != null && count == 1) {
                    redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
                }
                return count != null && count <= maxRequests;
            } catch (Exception ex) {
                log.warn("Redis rate limiter unavailable ({}). Falling back to local in-memory rate limiting.", ex.getMessage());
                return checkLocalRateLimit(clientIp);
            }
        }
        return checkLocalRateLimit(clientIp);
    }

    boolean checkLocalRateLimit(String clientIp) {
        long windowMillis = windowSeconds * 1000L;

        // Self-cleaning eviction if cache exceeds capacity threshold
        if (ipTrackers.size() > 5000) {
            long now = System.currentTimeMillis();
            ipTrackers.entrySet().removeIf(entry -> now - entry.getValue().windowStartTime > windowMillis);
        }

        WindowTracker tracker = ipTrackers.computeIfAbsent(clientIp, k -> new WindowTracker());
        return tracker.tryAcquire(maxRequests, windowMillis);
    }

    String getClientIp(HttpServletRequest request) {
        String remote = request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
        boolean fromTrustedProxy = trustedProxies.contains(remote);
        String xff = request.getHeader("X-Forwarded-For");

        if (fromTrustedProxy && xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return remote;
    }

    public void reset() {
        ipTrackers.clear();
        if (redisTemplate != null) {
            try {
                Set<String> keys = redisTemplate.keys("rl:login:*");
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                }
            } catch (Exception ex) {
                log.warn("Failed to reset Redis rate limit keys: {}", ex.getMessage());
            }
        }
    }

    static class WindowTracker {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStartTime = System.currentTimeMillis();

        synchronized boolean tryAcquire(int maxLimit, long windowMillis) {
            long now = System.currentTimeMillis();
            if (now - windowStartTime >= windowMillis) {
                windowStartTime = now;
                count.set(0);
            }
            return count.incrementAndGet() <= maxLimit;
        }
    }
}

