package com.taskmanager.security;

import com.taskmanager.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_ROLES = "roles";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final AppProperties properties;
    private final SecretKey key;

    public JwtService(AppProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, String email, List<String> roles) {
        return buildToken(userId, email, Map.of(CLAIM_TYPE, TYPE_ACCESS, CLAIM_ROLES, roles),
                properties.getJwt().getAccessTokenExpirationMs());
    }

    public String generateRefreshToken(Long userId, String email) {
        // Unique jti guarantees each refresh token is distinct even when issued within the same
        // second, so rotation reliably invalidates the previously stored token.
        return buildToken(userId, email, Map.of(CLAIM_TYPE, TYPE_REFRESH),
                properties.getJwt().getRefreshTokenExpirationMs(), UUID.randomUUID().toString());
    }

    private String buildToken(Long userId, String email, Map<String, Object> claims, long ttlMs) {
        return buildToken(userId, email, claims, ttlMs, UUID.randomUUID().toString());
    }

    private String buildToken(Long userId, String email, Map<String, Object> claims, long ttlMs, String jti) {
        Date now = new Date();
        return Jwts.builder()
                .id(jti)
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claims(claims)
                .issuer(properties.getJwt().getIssuer())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(properties.getJwt().getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isAccessToken(Claims claims) {
        return TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class));
    }

    public Long extractUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    public long getRefreshTokenExpirationMs() {
        return properties.getJwt().getRefreshTokenExpirationMs();
    }

    public long getAccessTokenExpirationMs() {
        return properties.getJwt().getAccessTokenExpirationMs();
    }

    public Date extractExpiration(Claims claims) {
        return claims.getExpiration();
    }
}
