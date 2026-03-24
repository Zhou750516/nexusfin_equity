package com.nexusfin.equity.util;

import com.nexusfin.equity.config.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private final AuthProperties authProperties;

    public JwtUtil(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public String generateToken(String memberId, String techPlatformUserId) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(authProperties.getJwt().getTtlSeconds());
        return Jwts.builder()
                .issuer(authProperties.getJwt().getIssuer())
                .subject(memberId)
                .claim("memberId", memberId)
                .claim("techPlatformUserId", techPlatformUserId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(getSigningKey())
                .compact();
    }

    public AuthPrincipal parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new AuthPrincipal(
                    claims.get("memberId", String.class),
                    claims.get("techPlatformUserId", String.class)
            );
        } catch (JwtException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid jwt token", exception);
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(authProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
