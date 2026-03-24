package com.nexusfin.equity.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.dto.response.Result;
import com.nexusfin.equity.util.AuthContextUtil;
import com.nexusfin.equity.util.AuthPrincipal;
import com.nexusfin.equity.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthProperties authProperties;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(
            AuthProperties authProperties,
            JwtUtil jwtUtil,
            ObjectMapper objectMapper
    ) {
        this.authProperties = authProperties;
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestUri = request.getRequestURI();
        if (shouldSkip(requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = resolveToken(request);
        if (token == null) {
            writeUnauthorized(response, "Missing auth cookie");
            return;
        }
        try {
            AuthPrincipal principal = jwtUtil.parseToken(token);
            AuthContextUtil.bind(principal);
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException exception) {
            writeUnauthorized(response, "Invalid auth cookie");
        } finally {
            AuthContextUtil.clear();
        }
    }

    private boolean shouldSkip(String requestUri) {
        boolean excluded = authProperties.getExcludedPathPrefixes().stream()
                .anyMatch(requestUri::startsWith);
        if (excluded) {
            return true;
        }
        return authProperties.getProtectedPathPrefixes().stream()
                .noneMatch(requestUri::startsWith);
    }

    private String resolveToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (authProperties.getJwt().getCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Result.failure(401, message));
    }
}
