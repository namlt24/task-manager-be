package com.taskmanager.auth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Stores a single-use password-reset code per email in Redis with a short TTL.
 * The code self-expires after {@link #CODE_TTL}; once consumed it is deleted so it cannot be reused.
 * Key: {@code pwd-reset:{email}} (email lower-cased by the caller).
 */
@Component
public class PasswordResetStore {

    public static final Duration CODE_TTL = Duration.ofMinutes(1);

    private static final String KEY_PREFIX = "pwd-reset:";

    private final StringRedisTemplate redis;

    public PasswordResetStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void save(String email, String code) {
        redis.opsForValue().set(key(email), code, CODE_TTL);
    }

    public String get(String email) {
        return redis.opsForValue().get(key(email));
    }

    public void delete(String email) {
        redis.delete(key(email));
    }

    /** Remaining lifetime of the current code in seconds, or -1 if none/expired. */
    public long ttlSeconds(String email) {
        Long ttl = redis.getExpire(key(email), TimeUnit.SECONDS);
        return ttl == null ? -1 : ttl;
    }

    private String key(String email) {
        return KEY_PREFIX + email.toLowerCase();
    }
}
