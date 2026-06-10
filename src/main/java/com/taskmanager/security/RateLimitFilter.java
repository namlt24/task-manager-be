package com.taskmanager.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * Per-IP brute-force protection for sensitive auth endpoints using a Redis fixed-window counter.
 * Returns 429 (ProblemDetail) with a {@code Retry-After} header once the limit is exceeded.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    /** Only the password-guessing-prone endpoints are limited (refresh is left alone). */
    private static final Set<String> LIMITED_SUFFIXES = Set.of(
            "/v1/auth/login", "/v1/auth/register", "/v1/auth/forgot-password", "/v1/auth/reset-password");

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final AppProperties.RateLimit config;

    public RateLimitFilter(StringRedisTemplate redis, ObjectMapper objectMapper, AppProperties.RateLimit config) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.config = config;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!config.isEnabled() || !isLimited(request)) {
            chain.doFilter(request, response);
            return;
        }

        String key = "rl:auth:" + clientIp(request) + ":" + lastSegment(request);
        Long count;
        try {
            count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redis.expire(key, Duration.ofSeconds(config.getWindowSeconds()));
            }
        } catch (Exception ex) {
            // If Redis is unavailable, fail open rather than blocking logins.
            chain.doFilter(request, response);
            return;
        }

        if (count != null && count > config.getAuthRequests()) {
            Long ttl = redis.getExpire(key);
            writeTooMany(response, ttl == null || ttl < 0 ? config.getWindowSeconds() : ttl);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isLimited(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return LIMITED_SUFFIXES.stream().anyMatch(uri::endsWith) && "POST".equalsIgnoreCase(request.getMethod());
    }

    private String lastSegment(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.substring(uri.lastIndexOf('/') + 1);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooMany(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        Map<String, Object> body = Map.of(
                "type", "about:blank",
                "title", "Too Many Requests",
                "status", 429,
                "detail", "Bạn thao tác quá nhanh. Vui lòng thử lại sau " + retryAfterSeconds + " giây.");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
