package com.arjunsports.contentagent.security.jwt;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET = "a-very-long-test-secret-that-is-at-least-32-bytes-long";

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setAccessTokenTtlMinutes(15);
        properties.setRefreshTokenTtlDays(7);

        jwtTokenProvider = new JwtTokenProvider(properties);
        jwtTokenProvider.init();
    }

    @Test
    void generateAccessToken_producesTokenWithExpectedClaims() {
        UUID userId = UUID.randomUUID();

        String token = jwtTokenProvider.generateAccessToken(userId, "admin@example.com", "ADMIN");

        assertThat(jwtTokenProvider.isValid(token)).isTrue();
        Claims claims = jwtTokenProvider.parseClaims(token);
        assertThat(jwtTokenProvider.getUserId(claims)).isEqualTo(userId);
        assertThat(jwtTokenProvider.getRole(claims)).isEqualTo("ADMIN");
    }

    @Test
    void isValid_returnsFalseForTamperedToken() {
        String token = jwtTokenProvider.generateAccessToken(UUID.randomUUID(), "admin@example.com", "ADMIN");
        String tampered = token.substring(0, token.length() - 4) + "abcd";

        assertThat(jwtTokenProvider.isValid(tampered)).isFalse();
    }

    @Test
    void isValid_returnsFalseForExpiredToken() {
        JwtProperties expiredProperties = new JwtProperties();
        expiredProperties.setSecret(SECRET);
        expiredProperties.setAccessTokenTtlMinutes(-1);
        expiredProperties.setRefreshTokenTtlDays(7);

        JwtTokenProvider expiredTokenProvider = new JwtTokenProvider(expiredProperties);
        expiredTokenProvider.init();

        String token = expiredTokenProvider.generateAccessToken(UUID.randomUUID(), "admin@example.com", "ADMIN");

        assertThat(expiredTokenProvider.isValid(token)).isFalse();
    }

    @Test
    void isValid_returnsFalseForGarbageInput() {
        assertThat(jwtTokenProvider.isValid("not-a-jwt")).isFalse();
    }
}
