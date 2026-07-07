package com.arjunsports.contentagent.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";

    private final JwtProperties jwtProperties;

    private SecretKey key;

    @PostConstruct
    void init() {
        this.key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId, String email, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtProperties.getAccessTokenTtlMinutes(), ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLE, role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public UUID getUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public String getRole(Claims claims) {
        return claims.get(CLAIM_ROLE, String.class);
    }

    public Duration getAccessTokenTtl() {
        return Duration.ofMinutes(jwtProperties.getAccessTokenTtlMinutes());
    }

    public Duration getRefreshTokenTtl() {
        return Duration.ofDays(jwtProperties.getRefreshTokenTtlDays());
    }
}
