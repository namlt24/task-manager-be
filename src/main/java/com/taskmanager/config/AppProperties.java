package com.taskmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Cors cors = new Cors();
    private final Mail mail = new Mail();
    private final Storage storage = new Storage();
    private final RateLimit rateLimit = new RateLimit();

    /** Public base URL of the SPA, used to build links in emails (e.g. invitation accept link). */
    private String frontendUrl = "http://localhost:4200";

    public String getFrontendUrl() {
        return frontendUrl;
    }

    public void setFrontendUrl(String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public Cors getCors() {
        return cors;
    }

    public Mail getMail() {
        return mail;
    }

    public Storage getStorage() {
        return storage;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    /** Brute-force protection for auth endpoints (per client IP, sliding fixed window in Redis). */
    public static class RateLimit {
        private boolean enabled = true;
        private int authRequests = 10;     // max requests per window
        private int windowSeconds = 60;    // window length

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getAuthRequests() {
            return authRequests;
        }

        public void setAuthRequests(int authRequests) {
            this.authRequests = authRequests;
        }

        public int getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(int windowSeconds) {
            this.windowSeconds = windowSeconds;
        }
    }

    public static class Jwt {
        private String secret;
        private long accessTokenExpirationMs = 900_000;
        private long refreshTokenExpirationMs = 604_800_000;
        private String issuer = "task-manager";

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getAccessTokenExpirationMs() {
            return accessTokenExpirationMs;
        }

        public void setAccessTokenExpirationMs(long accessTokenExpirationMs) {
            this.accessTokenExpirationMs = accessTokenExpirationMs;
        }

        public long getRefreshTokenExpirationMs() {
            return refreshTokenExpirationMs;
        }

        public void setRefreshTokenExpirationMs(long refreshTokenExpirationMs) {
            this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }
    }

    public static class Cors {
        private String allowedOrigins = "http://localhost:4200";

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Mail {
        private String from = "no-reply@taskmanager.local";

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }
    }

    public static class Storage {
        private String location = "./uploads";

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }
    }
}
