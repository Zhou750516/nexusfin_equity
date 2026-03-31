package com.nexusfin.equity.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nexusfin.auth")
public class AuthProperties {

    private String techPlatformBaseUrl;
    private String techPlatformUserMePath;
    private int connectTimeoutMs = 2000;
    private int readTimeoutMs = 3000;
    private int retryMaxAttempts = 2;
    private String defaultChannelCode = "KJ";
    private String defaultRedirectUrl = "/equity/index";
    private List<String> redirectWhitelist = new ArrayList<>();
    private List<String> protectedPathPrefixes = new ArrayList<>();
    private List<String> excludedPathPrefixes = new ArrayList<>();
    private Jwt jwt = new Jwt();

    public String getTechPlatformBaseUrl() {
        return techPlatformBaseUrl;
    }

    public void setTechPlatformBaseUrl(String techPlatformBaseUrl) {
        this.techPlatformBaseUrl = techPlatformBaseUrl;
    }

    public String getTechPlatformUserMePath() {
        return techPlatformUserMePath;
    }

    public void setTechPlatformUserMePath(String techPlatformUserMePath) {
        this.techPlatformUserMePath = techPlatformUserMePath;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public int getRetryMaxAttempts() {
        return retryMaxAttempts;
    }

    public void setRetryMaxAttempts(int retryMaxAttempts) {
        this.retryMaxAttempts = retryMaxAttempts;
    }

    public String getDefaultChannelCode() {
        return defaultChannelCode;
    }

    public void setDefaultChannelCode(String defaultChannelCode) {
        this.defaultChannelCode = defaultChannelCode;
    }

    public String getDefaultRedirectUrl() {
        return defaultRedirectUrl;
    }

    public void setDefaultRedirectUrl(String defaultRedirectUrl) {
        this.defaultRedirectUrl = defaultRedirectUrl;
    }

    public List<String> getRedirectWhitelist() {
        return redirectWhitelist;
    }

    public void setRedirectWhitelist(List<String> redirectWhitelist) {
        this.redirectWhitelist = redirectWhitelist;
    }

    public List<String> getProtectedPathPrefixes() {
        return protectedPathPrefixes;
    }

    public void setProtectedPathPrefixes(List<String> protectedPathPrefixes) {
        this.protectedPathPrefixes = protectedPathPrefixes;
    }

    public List<String> getExcludedPathPrefixes() {
        return excludedPathPrefixes;
    }

    public void setExcludedPathPrefixes(List<String> excludedPathPrefixes) {
        this.excludedPathPrefixes = excludedPathPrefixes;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public static class Jwt {

        private String issuer;
        private String secret;
        private long ttlSeconds = 7200L;
        private String cookieName = "NEXUSFIN_AUTH";
        private String cookiePath = "/";
        private boolean cookieSecure = true;
        private boolean cookieHttpOnly = true;
        private String cookieSameSite = "Lax";

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(long ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }

        public String getCookieName() {
            return cookieName;
        }

        public void setCookieName(String cookieName) {
            this.cookieName = cookieName;
        }

        public String getCookiePath() {
            return cookiePath;
        }

        public void setCookiePath(String cookiePath) {
            this.cookiePath = cookiePath;
        }

        public boolean isCookieSecure() {
            return cookieSecure;
        }

        public void setCookieSecure(boolean cookieSecure) {
            this.cookieSecure = cookieSecure;
        }

        public boolean isCookieHttpOnly() {
            return cookieHttpOnly;
        }

        public void setCookieHttpOnly(boolean cookieHttpOnly) {
            this.cookieHttpOnly = cookieHttpOnly;
        }

        public String getCookieSameSite() {
            return cookieSameSite;
        }

        public void setCookieSameSite(String cookieSameSite) {
            this.cookieSameSite = cookieSameSite;
        }
    }
}
