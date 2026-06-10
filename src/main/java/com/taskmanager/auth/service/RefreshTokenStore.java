package com.taskmanager.auth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

/**
 * Stores the currently-valid refresh token per user in Redis (whitelist + rotation).
 * Replacing the value on each refresh invalidates the previous token; deleting it logs the user out.
 */
@Component
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redis;

    public RefreshTokenStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void store(Long userId, String token, long ttlMs) {
        redis.opsForValue().set(key(userId), token, Duration.ofMillis(ttlMs));
    }

    public boolean isValid(Long userId, String token) {
        String stored = redis.opsForValue().get(key(userId));
        return Objects.equals(stored, token);
    }

    public void revoke(Long userId) {
        redis.delete(key(userId));
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
