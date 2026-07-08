package com.arjunsports.contentagent.modules.draft;

import com.arjunsports.contentagent.modules.ai.ContentType;
import com.arjunsports.contentagent.modules.ai.GeneratedContent;
import com.arjunsports.contentagent.modules.ai.GeneratedContentRepository;
import com.arjunsports.contentagent.modules.ai.GenerationStatus;
import com.arjunsports.contentagent.modules.auth.dto.LoginRequest;
import com.arjunsports.contentagent.modules.draft.dto.CreateDraftRequest;
import com.arjunsports.contentagent.modules.draft.dto.UpdateDraftRequest;
import com.arjunsports.contentagent.modules.user.Role;
import com.arjunsports.contentagent.modules.user.User;
import com.arjunsports.contentagent.modules.user.UserRepository;
import com.arjunsports.contentagent.modules.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DraftControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String EMAIL = "draft-admin@test.com";
    private static final String PASSWORD = "TestPassw0rd";

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private DraftRepository draftRepository;
    @Autowired
    private GeneratedContentRepository generatedContentRepository;

    // Static: shared across all test methods so we only ever log in once (LoginRateLimiter is 5/min/IP,
    // and every TestRestTemplate call in this suite originates from the same "client IP").
    private static String accessCookie;

    private UUID sourceContentId;

    @BeforeEach
    void setUp() {
        draftRepository.deleteAll();
        generatedContentRepository.deleteAll();

        if (userRepository.findByEmailIgnoreCase(EMAIL).isEmpty()) {
            User user = User.builder()
                    .firstName("Draft").lastName("Admin").email(EMAIL)
                    .password(passwordEncoder.encode(PASSWORD))
                    .role(Role.ADMIN).status(UserStatus.ACTIVE).active(true)
                    .build();
            userRepository.save(user);
        }

        GeneratedContent source = GeneratedContent.builder()
                .contentType(ContentType.INSTAGRAM_CAPTION)
                .promptUsed("prompt")
                .generatedText("Champions are made in practice.")
                .status(GenerationStatus.SUCCESS)
                .build();
        sourceContentId = generatedContentRepository.save(source).getId();

        if (accessCookie == null) {
            ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
                    "/api/auth/login", new LoginRequest(EMAIL, PASSWORD), Map.class);
            accessCookie = extractCookie(loginResponse, "access_token");
        }
    }

    @Test
    void create_success_returnsNewDraft() {
        ResponseEntity<Map> response = create(new CreateDraftRequest(sourceContentId, "My Draft"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = data(response);
        assertThat(data.get("title")).isEqualTo("My Draft");
        assertThat(data.get("status")).isEqualTo("DRAFT");
        assertThat(data.get("contentText")).isEqualTo("Champions are made in practice.");
    }

    @Test
    void create_unknownGeneratedContent_returnsNotFound() {
        ResponseEntity<Map> response = create(new CreateDraftRequest(UUID.randomUUID(), null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void create_withoutAuthentication_returnsUnauthorized() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/drafts", new CreateDraftRequest(sourceContentId, null), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void update_blankContentText_returnsBadRequest() {
        String id = createAndGetId();

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/drafts/" + id, HttpMethod.PUT,
                requestWithCookieAndBody(new UpdateDraftRequest("Title", "   ", null)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void update_invalidStatus_returnsBadRequest() {
        String id = createAndGetId();

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/drafts/" + id, HttpMethod.PUT,
                requestWithCookieAndBody(new UpdateDraftRequest("Title", "text", "NOPE")), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void update_success_changesTextAndStatus() {
        String id = createAndGetId();

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/drafts/" + id, HttpMethod.PUT,
                requestWithCookieAndBody(new UpdateDraftRequest("New Title", "New text", "READY_FOR_REVIEW")), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = data(response);
        assertThat(data.get("title")).isEqualTo("New Title");
        assertThat(data.get("status")).isEqualTo("READY_FOR_REVIEW");
    }

    @Test
    void delete_thenRestore_roundTrips() {
        String id = createAndGetId();

        ResponseEntity<Map> deleteResponse = restTemplate.exchange(
                "/api/drafts/" + id, HttpMethod.DELETE, requestWithCookie(), Map.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> getAfterDelete = restTemplate.exchange(
                "/api/drafts/" + id, HttpMethod.GET, requestWithCookie(), Map.class);
        assertThat(data(getAfterDelete).get("deleted")).isEqualTo(true);

        ResponseEntity<Map> restoreResponse = restTemplate.exchange(
                "/api/drafts/" + id + "/restore", HttpMethod.POST, requestWithCookie(), Map.class);
        assertThat(restoreResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(restoreResponse).get("deleted")).isEqualTo(false);
    }

    @Test
    void delete_alreadyDeleted_returnsBadRequest() {
        String id = createAndGetId();
        restTemplate.exchange("/api/drafts/" + id, HttpMethod.DELETE, requestWithCookie(), Map.class);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/drafts/" + id, HttpMethod.DELETE, requestWithCookie(), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void duplicate_success_createsIndependentCopy() {
        String id = createAndGetId();

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/drafts/" + id + "/duplicate", HttpMethod.POST, requestWithCookie(), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = data(response);
        assertThat(data.get("id")).isNotEqualTo(id);
        assertThat(data.get("status")).isEqualTo("DRAFT");
    }

    @Test
    void finalize_thenFinalizeAnotherDraftOfSameSource_returnsConflict() {
        String firstId = createAndGetId();
        restTemplate.exchange("/api/drafts/" + firstId + "/finalize", HttpMethod.POST, requestWithCookie(), Map.class);

        String secondId = createAndGetId();
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/drafts/" + secondId + "/finalize", HttpMethod.POST, requestWithCookie(), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void list_filterByStatus_returnsOnlyMatching() {
        String id = createAndGetId();
        restTemplate.exchange("/api/drafts/" + id + "/finalize", HttpMethod.POST, requestWithCookie(), Map.class);
        createAndGetId(); // a second, still-DRAFT draft

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/drafts?status=APPROVED", HttpMethod.GET, requestWithCookie(), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = data(response);
        List<?> content = (List<?>) data.get("content");
        assertThat(content).hasSize(1);
    }

    @Test
    void list_searchByTitle_returnsMatching() {
        create(new CreateDraftRequest(sourceContentId, "Regional Championship Recap"));
        create(new CreateDraftRequest(sourceContentId, "Training Tip of the Week"));

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/drafts?search=championship", HttpMethod.GET, requestWithCookie(), Map.class);

        Map<String, Object> data = data(response);
        List<?> content = (List<?>) data.get("content");
        assertThat(content).hasSize(1);
    }

    @Test
    void list_invalidStatusFilter_returnsBadRequest() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/drafts?status=NOT_REAL", HttpMethod.GET, requestWithCookie(), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<Map> create(CreateDraftRequest request) {
        return restTemplate.exchange("/api/drafts", HttpMethod.POST, requestWithCookieAndBody(request), Map.class);
    }

    private String createAndGetId() {
        ResponseEntity<Map> response = create(new CreateDraftRequest(sourceContentId, null));
        return (String) data(response).get("id");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(ResponseEntity<Map> response) {
        return (Map<String, Object>) response.getBody().get("data");
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
}
