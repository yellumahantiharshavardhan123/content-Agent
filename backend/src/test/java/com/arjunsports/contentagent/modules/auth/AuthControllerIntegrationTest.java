package com.arjunsports.contentagent.modules.auth;

import com.arjunsports.contentagent.modules.auth.dto.LoginRequest;
import com.arjunsports.contentagent.modules.user.Role;
import com.arjunsports.contentagent.modules.user.User;
import com.arjunsports.contentagent.modules.user.UserRepository;
import com.arjunsports.contentagent.modules.user.UserStatus;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String EMAIL = "admin@test.com";
    private static final String PASSWORD = "TestPassw0rd";

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private UUID userId;

    @BeforeEach
    void seedUser() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        User user = User.builder()
                .firstName("Test")
                .lastName("Admin")
                .email(EMAIL)
                .password(passwordEncoder.encode(PASSWORD))
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .active(true)
                .build();
        userId = userRepository.save(user).getId();
    }

    @Test
    void login_success_setsHttpOnlyCookiesAndReturnsUser() {
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/auth/login", new LoginRequest(EMAIL, PASSWORD), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).isNotNull();
        assertThat(cookies.toString()).contains("access_token=").contains("refresh_token=").contains("HttpOnly");
    }

    @Test
    void login_wrongPassword_returnsUnauthorized() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(EMAIL, "wrong-password"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void me_withoutCookie_returnsUnauthorized() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/auth/me", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void me_withValidAccessTokenCookie_returnsCurrentUser() {
        String accessTokenCookie = extractCookie(login(), "access_token");

        HttpEntity<Void> request = requestWithCookie(accessTokenCookie);
        ResponseEntity<Map> response = restTemplate.exchange("/api/auth/me", HttpMethod.GET, request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("email")).isEqualTo(EMAIL);
    }

    @Test
    void me_withExpiredAccessToken_returnsUnauthorized() {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject(userId.toString())
                .claim("email", EMAIL)
                .claim("role", "ADMIN")
                .issuedAt(Date.from(Instant.now().minus(1, ChronoUnit.HOURS)))
                .expiration(Date.from(Instant.now().minus(30, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();

        HttpEntity<Void> request = requestWithCookie("access_token=" + expiredToken);
        ResponseEntity<Map> response = restTemplate.exchange("/api/auth/me", HttpMethod.GET, request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refresh_rotatesTokenAndRejectsReuseOfOldToken() {
        ResponseEntity<Map> loginResponse = login();
        String refreshCookie = extractCookie(loginResponse, "refresh_token");

        HttpEntity<Void> refreshRequest = requestWithCookie(refreshCookie);
        ResponseEntity<Map> firstRefresh = restTemplate.exchange("/api/auth/refresh", HttpMethod.POST, refreshRequest, Map.class);
        assertThat(firstRefresh.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Reusing the same (now-rotated-out) refresh token must be rejected.
        ResponseEntity<Map> reuseAttempt = restTemplate.exchange("/api/auth/refresh", HttpMethod.POST, refreshRequest, Map.class);
        assertThat(reuseAttempt.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logout_revokesRefreshTokenSoItCannotBeReused() {
        ResponseEntity<Map> loginResponse = login();
        String accessCookie = extractCookie(loginResponse, "access_token");
        String refreshCookie = extractCookie(loginResponse, "refresh_token");

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, accessCookie);
        headers.add(HttpHeaders.COOKIE, refreshCookie);
        HttpEntity<Void> logoutRequest = new HttpEntity<>(headers);

        ResponseEntity<Map> logoutResponse = restTemplate.exchange("/api/auth/logout", HttpMethod.POST, logoutRequest, Map.class);
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        HttpEntity<Void> refreshAfterLogout = requestWithCookie(refreshCookie);
        ResponseEntity<Map> refreshResponse = restTemplate.exchange("/api/auth/refresh", HttpMethod.POST, refreshAfterLogout, Map.class);
        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<Map> login() {
        return restTemplate.postForEntity("/api/auth/login", new LoginRequest(EMAIL, PASSWORD), Map.class);
    }

    private HttpEntity<Void> requestWithCookie(String cookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);
        return new HttpEntity<>(headers);
    }

    private String extractCookie(ResponseEntity<Map> response, String name) {
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).isNotNull();
        return cookies.stream()
                .filter(cookie -> cookie.startsWith(name + "="))
                .findFirst()
                .map(cookie -> cookie.split(";")[0])
                .orElseThrow(() -> new AssertionError("Cookie " + name + " not present in response"));
    }
}
