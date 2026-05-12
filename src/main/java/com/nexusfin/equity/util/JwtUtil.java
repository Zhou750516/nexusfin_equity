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

    private static final int MIN_HMAC_KEY_BYTES = 32;

    private final AuthProperties authProperties;
    private final SecretKey signingKey;

    public JwtUtil(AuthProperties authProperties) {
        this.authProperties = authProperties;
        this.signingKey = buildSigningKey(authProperties.getJwt().getSecret());
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
                .signWith(signingKey)
                .compact();
    }

    public AuthPrincipal parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
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

    private SecretKey buildSigningKey(String secret) {
        byte[] secretBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_HMAC_KEY_BYTES) {
            int bitLength = secretBytes.length * 8;
            throw new IllegalStateException(
                    "AUTH_JWT_SECRET is too weak: current length is "
                            + secretBytes.length + " bytes (" + bitLength + " bits), "
                            + "but JWT HMAC secret must be at least 32 bytes (256 bits). "
                            + "Generate a high-entropy secret, for example: openssl rand -base64 32"
            );
        }
        return Keys.hmacShaKeyFor(secretBytes);
    }
}
