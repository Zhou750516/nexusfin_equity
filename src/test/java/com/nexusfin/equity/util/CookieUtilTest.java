package com.nexusfin.equity.util;

import com.nexusfin.equity.config.AuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import static org.assertj.core.api.Assertions.assertThat;

class CookieUtilTest {

    @Test
    void shouldBuildSecureHttpOnlyCookie() {
        CookieUtil cookieUtil = new CookieUtil(authProperties());

        ResponseCookie cookie = cookieUtil.buildAuthCookie("jwt-token");

        assertThat(cookie.getName()).isEqualTo("NEXUSFIN_AUTH");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isFalse();
        assertThat(cookie.getValue()).isEqualTo("jwt-token");
        assertThat(cookie.toString()).contains("SameSite=Lax");
    }

    private AuthProperties authProperties() {
        AuthProperties authProperties = new AuthProperties();
        AuthProperties.Jwt jwt = new AuthProperties.Jwt();
        jwt.setCookieName("NEXUSFIN_AUTH");
        jwt.setCookiePath("/");
        jwt.setCookieHttpOnly(true);
        jwt.setCookieSecure(false);
        jwt.setCookieSameSite("Lax");
        jwt.setTtlSeconds(1800);
        authProperties.setJwt(jwt);
        return authProperties;
    }
}
