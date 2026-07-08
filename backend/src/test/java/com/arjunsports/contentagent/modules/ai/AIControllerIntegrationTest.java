package com.arjunsports.contentagent.modules.ai;

import com.arjunsports.contentagent.modules.ai.dto.GenerateContentRequest;
import com.arjunsports.contentagent.modules.ai.dto.RegenerateContentRequest;
import com.arjunsports.contentagent.modules.ai.provider.AIGenerationRequest;
import com.arjunsports.contentagent.modules.ai.provider.AIGenerationResult;
import com.arjunsports.contentagent.modules.ai.provider.AIProvider;
import com.arjunsports.contentagent.modules.ai.provider.AIProviderException;
import com.arjunsports.contentagent.modules.auth.dto.LoginRequest;
import com.arjunsports.contentagent.modules.user.Role;
import com.arjunsports.contentagent.modules.user.User;
import com.arjunsports.contentagent.modules.user.UserRepository;
import com.arjunsports.contentagent.modules.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@ActiveProfiles("test")
@TestPropertySource(properties = "app.ai.active-provider=fake-test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AIControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String EMAIL = "ai-admin@test.com";
    private static final String PASSWORD = "TestPassw0rd";

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private PromptRepository promptRepository;
    @Autowired
    private GeneratedContentRepository generatedContentRepository;
    @Autowired
    private GenerationHistoryRepository generationHistoryRepository;

    // Static: shared across all test methods in this class so we only ever log in once.
    // LoginRateLimiter (5/min/IP) would otherwise reject the Nth test's login, since every
    // TestRestTemplate call in this suite originates from the same "client IP".
    private static String accessCookie;

    @BeforeEach
    void setUp() {
        generatedContentRepository.deleteAll();
        promptRepository.deleteAll();

        if (userRepository.findByEmailIgnoreCase(EMAIL).isEmpty()) {
            User user = User.builder()
                    .firstName("AI").lastName("Admin").email(EMAIL)
                    .password(passwordEncoder.encode(PASSWORD))
                    .role(Role.ADMIN).status(UserStatus.ACTIVE).active(true)
                    .build();
            userRepository.save(user);
        }

        promptRepository.save(PromptTemplate.builder()
                .contentType(ContentType.HASHTAGS)
                .name("Hashtags")
                .systemPrompt("system")
                .userPromptTemplate("Notes: {{manualNotes}}")
                .version(1)
                .active(true)
                .build());

        if (accessCookie == null) {
            ResponseEntity<Map> loginResponse = restTemplate.postForEntity("/api/auth/login", new LoginRequest(EMAIL, PASSWORD), Map.class);
            accessCookie = extractCookie(loginResponse, "access_token");
        }
    }

    @Test
    void generate_success_returnsGeneratedContent() {
        GenerateContentRequest request = new GenerateContentRequest(
                null, ContentType.HASHTAGS, "great training session", null, null, null, null, null);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/ai/generate", HttpMethod.POST, requestWithCookieAndBody(request), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("SUCCESS");
        assertThat(data.get("generatedText")).isEqualTo("#fake #generated #hashtags");
    }

    @Test
    void generate_noContext_returnsBadRequest() {
        GenerateContentRequest request = new GenerateContentRequest(
                null, ContentType.HASHTAGS, null, null, null, null, null, null);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/ai/generate", HttpMethod.POST, requestWithCookieAndBody(request), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void generate_withoutAuthentication_returnsUnauthorized() {
        GenerateContentRequest request = new GenerateContentRequest(
                null, ContentType.HASHTAGS, "notes", null, null, null, null, null);

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/ai/generate", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void regenerate_afterGenerate_updatesSameContentRow() {
        GenerateContentRequest generateRequest = new GenerateContentRequest(
                null, ContentType.HASHTAGS, "great training session", null, null, null, null, null);
        ResponseEntity<Map> generateResponse = restTemplate.exchange(
                "/api/ai/generate", HttpMethod.POST, requestWithCookieAndBody(generateRequest), Map.class);
        String contentId = (String) ((Map<String, Object>) generateResponse.getBody().get("data")).get("id");

        ResponseEntity<Map> regenerateResponse = restTemplate.exchange(
                "/api/ai/regenerate", HttpMethod.POST,
                requestWithCookieAndBody(new RegenerateContentRequest(UUID.fromString(contentId))), Map.class);

        assertThat(regenerateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) regenerateResponse.getBody().get("data");
        assertThat(data.get("id")).isEqualTo(contentId);
    }

    @Test
    void generate_providerFailure_returns503AndPersistsFailedHistoryDespiteRollback() {
        GenerateContentRequest request = new GenerateContentRequest(
                null, ContentType.HASHTAGS, "TRIGGER_FAILURE", null, null, null, null, null);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/ai/generate", HttpMethod.POST, requestWithCookieAndBody(request), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        // The critical assertion: this failure record must survive even though the
        // @Transactional generate() method rolled back after re-throwing (see
        // GenerationFailureRecorder's REQUIRES_NEW - this test guards that regression).
        List<GenerationHistory> history = generationHistoryRepository.findAll();
        assertThat(history)
                .anyMatch(h -> h.getStatus() == GenerationStatus.FAILED && h.getAction() == GenerationAction.GENERATE);
    }

    @Test
    void history_returnsRecordedAttempts() {
        GenerateContentRequest request = new GenerateContentRequest(
                null, ContentType.HASHTAGS, "notes", null, null, null, null, null);
        restTemplate.exchange("/api/ai/generate", HttpMethod.POST, requestWithCookieAndBody(request), Map.class);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/ai/history", HttpMethod.GET, requestWithCookie(), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        List<?> content = (List<?>) data.get("content");
        assertThat(content).isNotEmpty();
    }

    private <T> HttpEntity<T> requestWithCookieAndBody(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, accessCookie);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<Void> requestWithCookie() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, accessCookie);
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

    @TestConfiguration
    static class FakeAIProviderConfig {

        @Bean
        @Primary
        public AIProvider fakeTestAIProvider() {
            return new AIProvider() {
                @Override
                public String getProviderName() {
                    return "fake-test";
                }

                @Override
                public AIGenerationResult generate(AIGenerationRequest request) {
                    if (request.userPrompt() != null && request.userPrompt().contains("TRIGGER_FAILURE")) {
                        throw new AIProviderException(AIProviderException.Reason.UNAVAILABLE, "simulated provider outage");
                    }
                    return AIGenerationResult.builder()
                            .rawContent("#fake #generated #hashtags")
                            .modelUsed("fake-test-model")
                            .build();
                }
            };
        }
    }
}
