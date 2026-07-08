package com.arjunsports.contentagent.modules.instagram;

import com.arjunsports.contentagent.modules.ai.ContentType;
import com.arjunsports.contentagent.modules.approval.entity.Approval;
import com.arjunsports.contentagent.modules.approval.entity.ApprovalStatus;
import com.arjunsports.contentagent.modules.approval.repository.ApprovalRepository;
import com.arjunsports.contentagent.modules.auth.dto.LoginRequest;
import com.arjunsports.contentagent.modules.draft.ContentDraft;
import com.arjunsports.contentagent.modules.draft.DraftRepository;
import com.arjunsports.contentagent.modules.draft.DraftStatus;
import com.arjunsports.contentagent.modules.instagram.dto.ConnectAccountRequest;
import com.arjunsports.contentagent.modules.instagram.dto.PublishRequest;
import com.arjunsports.contentagent.modules.instagram.entity.InstagramPost;
import com.arjunsports.contentagent.modules.instagram.entity.InstagramPostStatus;
import com.arjunsports.contentagent.modules.instagram.provider.InstagramAccountInfo;
import com.arjunsports.contentagent.modules.instagram.provider.InstagramPublishRequest;
import com.arjunsports.contentagent.modules.instagram.provider.InstagramPublishResult;
import com.arjunsports.contentagent.modules.instagram.provider.InstagramPublisher;
import com.arjunsports.contentagent.modules.instagram.provider.InstagramPublisherException;
import com.arjunsports.contentagent.modules.instagram.repository.InstagramAccountRepository;
import com.arjunsports.contentagent.modules.instagram.repository.InstagramPostRepository;
import com.arjunsports.contentagent.modules.instagram.repository.InstagramPublishHistoryRepository;
import com.arjunsports.contentagent.modules.media.Media;
import com.arjunsports.contentagent.modules.media.MediaRepository;
import com.arjunsports.contentagent.modules.media.MediaType;
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
@TestPropertySource(properties = "app.instagram.active-publisher=mock")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InstagramControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String EMAIL = "instagram-admin@test.com";
    private static final String PASSWORD = "TestPassw0rd";

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ApprovalRepository approvalRepository;
    @Autowired
    private DraftRepository draftRepository;
    @Autowired
    private MediaRepository mediaRepository;
    @Autowired
    private InstagramAccountRepository accountRepository;
    @Autowired
    private InstagramPostRepository postRepository;
    @Autowired
    private InstagramPublishHistoryRepository historyRepository;

    private static String accessCookie;

    private UUID approvalId;
    private UUID mediaId;

    @BeforeEach
    void setUp() {
        historyRepository.deleteAll();
        postRepository.deleteAll();
        accountRepository.deleteAll();
        approvalRepository.deleteAll();
        draftRepository.deleteAll();
        mediaRepository.deleteAll();

        if (userRepository.findByEmailIgnoreCase(EMAIL).isEmpty()) {
            User user = User.builder()
                    .firstName("Instagram").lastName("Admin").email(EMAIL)
                    .password(passwordEncoder.encode(PASSWORD))
                    .role(Role.ADMIN).status(UserStatus.ACTIVE).active(true)
                    .build();
            userRepository.save(user);
        }

        UUID draftId = draftRepository.save(ContentDraft.builder()
                .contentType(ContentType.INSTAGRAM_CAPTION)
                .title("Regional Championship Recap")
                .contentText("Our athletes brought home the gold this weekend.")
                .status(DraftStatus.APPROVED)
                .build()).getId();

        Approval approval = Approval.builder()
                .contentId(draftId)
                .contentTitle("Regional Championship Recap")
                .status(ApprovalStatus.READY_FOR_PUBLISH)
                .build();
        approvalId = approvalRepository.save(approval).getId();

        Media media = Media.builder()
                .fileName("trophy.jpg")
                .storageKey("test/trophy-" + UUID.randomUUID() + ".jpg")
                .contentType("image/jpeg")
                .mediaType(MediaType.IMAGE)
                .fileSizeBytes(2048L)
                .build();
        mediaId = mediaRepository.save(media).getId();

        if (accessCookie == null) {
            ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
                    "/api/auth/login", new LoginRequest(EMAIL, PASSWORD), Map.class);
            accessCookie = extractCookie(loginResponse, "access_token");
        }
    }

    @Test
    void status_notConnected_returnsConnectedFalse() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/instagram/status", HttpMethod.GET, requestWithCookie(), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(response).get("connected")).isEqualTo(false);
    }

    @Test
    void connect_success_returnsConnectedTrueAndNeverExposesToken() {
        ResponseEntity<Map> response = connect();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = data(response);
        assertThat(data.get("connected")).isEqualTo(true);
        assertThat(data.get("username")).isEqualTo("fake_verified_account");
        assertThat(data.toString()).doesNotContain("real-secret-access-token");
    }

    @Test
    void connect_alreadyConnected_returnsConflict() {
        connect();

        ResponseEntity<Map> response = connect();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void connect_withoutAuthentication_returnsUnauthorized() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/instagram/connect", new ConnectAccountRequest("123", null, "token"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void publish_noAccountConnected_returnsConflict() {
        ResponseEntity<Map> response = publish(approvalId, mediaId, "A great caption", "#win");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void publish_success_persistsPublishedPostAndHistory() {
        connect();

        ResponseEntity<Map> response = publish(approvalId, mediaId, "A great caption", "#win");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = data(response);
        assertThat(data.get("status")).isEqualTo("PUBLISHED");
        assertThat(data.get("instagramMediaId")).isEqualTo("fake_media_123");

        ResponseEntity<Map> history = restTemplate.exchange(
                "/api/instagram/history", HttpMethod.GET, requestWithCookie(), Map.class);
        // Global history includes both the earlier CONNECT and this PUBLISH_SUCCESS entry.
        List<?> content = (List<?>) data(history).get("content");
        assertThat(content).hasSize(2);
        assertThat(content).anyMatch(entry -> "PUBLISH_SUCCESS".equals(((Map<?, ?>) entry).get("action")));
        assertThat(content).anyMatch(entry -> "CONNECT".equals(((Map<?, ?>) entry).get("action")));
    }

    @Test
    void publish_mockEndpoint_alwaysUsesMockRegardlessOfConfiguredPublisher() {
        connect();

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/instagram/publish/mock", HttpMethod.POST,
                requestWithCookieAndBody(new PublishRequest(approvalId, mediaId, "A caption", null)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(response).get("publisherName")).isEqualTo("mock");
    }

    @Test
    void publish_approvalNotReadyForPublish_returnsBadRequest() {
        connect();
        Approval pending = approvalRepository.save(Approval.builder()
                .contentTitle("Not ready yet")
                .status(ApprovalStatus.PENDING_APPROVAL)
                .build());

        ResponseEntity<Map> response = publish(pending.getId(), mediaId, "cap", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void publish_providerFailure_returns503AndPersistsFailedHistoryDespiteRollback() {
        connect();

        ResponseEntity<Map> response = publish(approvalId, mediaId, "please TRIGGER_FAILURE now", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        List<InstagramPost> posts = postRepository.findAll();
        assertThat(posts).anyMatch(p -> p.getStatus() == InstagramPostStatus.FAILED);
    }

    @Test
    void publish_alreadyPublished_returnsConflict() {
        connect();
        publish(approvalId, mediaId, "first publish", null);

        ResponseEntity<Map> response = publish(approvalId, mediaId, "second attempt", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void disconnect_success_clearsActiveAccount() {
        connect();

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/instagram/disconnect", HttpMethod.DELETE, requestWithCookie(), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<Map> status = restTemplate.exchange(
                "/api/instagram/status", HttpMethod.GET, requestWithCookie(), Map.class);
        assertThat(data(status).get("connected")).isEqualTo(false);
    }

    @Test
    void disconnect_notConnected_returnsBadRequest() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/instagram/disconnect", HttpMethod.DELETE, requestWithCookie(), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<Map> connect() {
        return restTemplate.exchange("/api/instagram/connect", HttpMethod.POST,
                requestWithCookieAndBody(new ConnectAccountRequest("123456789", "page-1", "real-secret-access-token")),
                Map.class);
    }

    private ResponseEntity<Map> publish(UUID approvalId, UUID mediaId, String caption, String hashtags) {
        return restTemplate.exchange("/api/instagram/publish", HttpMethod.POST,
                requestWithCookieAndBody(new PublishRequest(approvalId, mediaId, caption, hashtags)), Map.class);
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

    @TestConfiguration
    static class FakeInstagramPublisherConfig {

        @Bean
        @Primary
        public InstagramPublisher fakeMockInstagramPublisher() {
            return new InstagramPublisher() {
                @Override
                public String getPublisherName() {
                    return "mock";
                }

                @Override
                public InstagramAccountInfo verifyAccount(String businessAccountId, String accessToken) {
                    return InstagramAccountInfo.builder()
                            .businessAccountId(businessAccountId)
                            .username("fake_verified_account")
                            .build();
                }

                @Override
                public InstagramPublishResult publish(InstagramPublishRequest request) {
                    if (request.caption() != null && request.caption().contains("TRIGGER_FAILURE")) {
                        throw new InstagramPublisherException(InstagramPublisherException.Reason.UNAVAILABLE,
                                "simulated provider outage");
                    }
                    return InstagramPublishResult.builder()
                            .instagramMediaId("fake_media_123")
                            .permalink("https://instagram.com/p/fake_media_123/")
                            .build();
                }
            };
        }
    }
}
