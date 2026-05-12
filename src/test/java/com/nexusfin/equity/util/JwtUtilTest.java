package com.nexusfin.equity.util;

import com.nexusfin.equity.config.AuthProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    @Test
    void shouldGenerateAndParseJwtToken() {
        JwtUtil jwtUtil = new JwtUtil(authProperties());

        String token = jwtUtil.generateToken("mem-1", "tech-1");
        AuthPrincipal principal = jwtUtil.parseToken(token);

        assertThat(principal.memberId()).isEqualTo("mem-1");
        assertThat(principal.techPlatformUserId()).isEqualTo("tech-1");
    }

    @Test
    void shouldRejectInvalidToken() {
        JwtUtil jwtUtil = new JwtUtil(authProperties());

        assertThatThrownBy(() -> jwtUtil.parseToken("invalid-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid jwt token");
    }

    @Test
    void shouldFailFastWhenJwtSecretIsTooShort() {
        AuthProperties authProperties = new AuthProperties();
        AuthProperties.Jwt jwt = new AuthProperties.Jwt();
        jwt.setIssuer("test-issuer");
        jwt.setSecret("short!");
        authProperties.setJwt(jwt);

        assertThatThrownBy(() -> new JwtUtil(authProperties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AUTH_JWT_SECRET")
                .hasMessageContaining("32 bytes")
                .hasMessageContaining("256 bits")
                .hasMessageContaining("openssl rand -base64 32");
    }

    private AuthProperties authProperties() {
        AuthProperties authProperties = new AuthProperties();
        AuthProperties.Jwt jwt = new AuthProperties.Jwt();
        jwt.setIssuer("test-issuer");
        jwt.setSecret("test-jwt-secret-key-test-jwt-secret-key");
        jwt.setTtlSeconds(3600);
        authProperties.setJwt(jwt);
        return authProperties;
    }
}
