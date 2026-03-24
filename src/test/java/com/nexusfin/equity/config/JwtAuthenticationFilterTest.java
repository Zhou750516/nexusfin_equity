package com.nexusfin.equity.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.util.JwtUtil;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    @Test
    void shouldRejectProtectedRequestWithoutCookie() throws ServletException, IOException {
        AuthProperties authProperties = authProperties();
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                authProperties,
                new JwtUtil(authProperties),
                new ObjectMapper()
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Missing auth cookie");
    }

    @Test
    void shouldSkipExcludedRequestPath() throws ServletException, IOException {
        AuthProperties authProperties = authProperties();
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                authProperties,
                new JwtUtil(authProperties),
                new ObjectMapper()
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/equity/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    private AuthProperties authProperties() {
        AuthProperties authProperties = new AuthProperties();
        authProperties.getProtectedPathPrefixes().add("/api/equity");
        authProperties.getProtectedPathPrefixes().add("/api/users/me");
        authProperties.getExcludedPathPrefixes().add("/api/equity/health");
        AuthProperties.Jwt jwt = new AuthProperties.Jwt();
        jwt.setIssuer("test-issuer");
        jwt.setSecret("test-jwt-secret-key-test-jwt-secret-key");
        jwt.setCookieName("NEXUSFIN_AUTH");
        authProperties.setJwt(jwt);
        return authProperties;
    }
}
