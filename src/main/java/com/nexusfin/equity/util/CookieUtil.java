package com.nexusfin.equity.util;

import com.nexusfin.equity.config.AuthProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    private final AuthProperties authProperties;

    public CookieUtil(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public ResponseCookie buildAuthCookie(String token) {
        return ResponseCookie.from(authProperties.getJwt().getCookieName(), token)
                .httpOnly(authProperties.getJwt().isCookieHttpOnly())
                .secure(authProperties.getJwt().isCookieSecure())
                .sameSite(authProperties.getJwt().getCookieSameSite())
                .path(authProperties.getJwt().getCookiePath())
                .maxAge(authProperties.getJwt().getTtlSeconds())
                .build();
    }

    public ResponseCookie expireAuthCookie() {
        return ResponseCookie.from(authProperties.getJwt().getCookieName(), "")
                .httpOnly(authProperties.getJwt().isCookieHttpOnly())
                .secure(authProperties.getJwt().isCookieSecure())
                .sameSite(authProperties.getJwt().getCookieSameSite())
                .path(authProperties.getJwt().getCookiePath())
                .maxAge(0)
                .build();
    }
}
