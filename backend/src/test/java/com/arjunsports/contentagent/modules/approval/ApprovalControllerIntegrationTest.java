package com.arjunsports.contentagent.modules.approval;

import com.arjunsports.contentagent.modules.ai.ContentType;
import com.arjunsports.contentagent.modules.approval.dto.AddCommentRequest;
import com.arjunsports.contentagent.modules.approval.dto.ApproveRequest;
import com.arjunsports.contentagent.modules.approval.dto.RejectRequest;
import com.arjunsports.contentagent.modules.approval.dto.SubmitApprovalRequest;
import com.arjunsports.contentagent.modules.approval.repository.ApprovalCommentRepository;
import com.arjunsports.contentagent.modules.approval.repository.ApprovalHistoryRepository;
import com.arjunsports.contentagent.modules.approval.repository.ApprovalRepository;
import com.arjunsports.contentagent.modules.auth.dto.LoginRequest;
import com.arjunsports.contentagent.modules.draft.ContentDraft;
import com.arjunsports.contentagent.modules.draft.DraftRepository;
import com.arjunsports.contentagent.modules.draft.DraftStatus;
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
class ApprovalControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String EMAIL = "approval-admin@test.com";
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
    private ApprovalRepository approvalRepository;
    @Autowired
    private ApprovalHistoryRepository approvalHistoryRepository;
    @Autowired
    private ApprovalCommentRepository approvalCommentRepository;

    // Static: shared across all test methods so we only ever log in once (LoginRateLimiter is 5/min/IP).
    private static String accessCookie;

    private UUID draftId;

    @BeforeEach
    void setUp() {
        approvalCommentRepository.deleteAll();
        approvalHistoryRepository.deleteAll();
        approvalRepository.deleteAll();
        draftRepository.deleteAll();

        if (userRepository.findByEmailIgnoreCase(EMAIL).isEmpty()) {
            User user = User.builder()
                    .firstName("Approval").lastName("Admin").email(EMAIL)
                    .password(passwordEncoder.encode(PASSWORD))
                    .role(Role.ADMIN).status(UserStatus.ACTIVE).active(true)
                    .build();
            userRepository.save(user);
        }

        ContentDraft draft = ContentDraft.builder()
                .contentType(ContentType.INSTAGRAM_CAPTION)
                .title("Regional Championship Recap")
                .contentText("Our athletes brought home the gold this weekend.")
                .status(DraftStatus.DRAFT)
                .build();
        draftId = draftRepository.save(draft).getId();

        if (accessCookie == null) {
            ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
                    "/api/auth/login", new LoginRequest(EMAIL, PASSWORD), Map.class);
            accessCookie = extractCookie(loginResponse, "access_token");
        }
    }

    @Test
    void submit_success_setsPendingApprovalAndUpdatesDraftStatus() {
        ResponseEntity<Map> response = submit(draftId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = data(response);
        assertThat(data.get("status")).isEqualTo("PENDING_APPROVAL");
        assertThat(data.get("contentTitle")).isEqualTo("Regional Championship Recap");

        ContentDraft reloaded = draftRepository.findById(draftId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(DraftStatus.READY_FOR_REVIEW);
    }

    @Test
    void submit_duplicatePending_returnsConflict() {
        submit(draftId);

        ResponseEntity<Map> response = submit(draftId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void submit_deletedDraft_returnsBadRequest() {
        ContentDraft draft = draftRepository.findById(draftId).orElseThrow();
        draft.setDeleted(true);
        draftRepository.save(draft);

        ResponseEntity<Map> response = submit(draftId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void submit_withoutAuthentication_returnsUnauthorized() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/approval/submit", new SubmitApprovalRequest(draftId), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void approve_success_setsReadyForPublishAndApprovesDraft() {
        String approvalId = (String) data(submit(draftId)).get("id");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/approval/approve/" + approvalId, HttpMethod.POST,
                requestWithCookieAndBody(new ApproveRequest("Great work")), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(response).get("status")).isEqualTo("READY_FOR_PUBLISH");

        ContentDraft reloaded = draftRepository.findById(draftId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(DraftStatus.APPROVED);
    }

    @Test
    void approve_alreadyApproved_returnsConflict() {
        String approvalId = (String) data(submit(draftId)).get("id");
        restTemplate.exchange("/api/approval/approve/" + approvalId, HttpMethod.POST,
                requestWithCookieAndBody(new ApproveRequest(null)), Map.class);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/approval/approve/" + approvalId, HttpMethod.POST,
                requestWithCookieAndBody(new ApproveRequest(null)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void reject_success_returnsDraftToDraftStatus() {
        String approvalId = (String) data(submit(draftId)).get("id");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/approval/reject/" + approvalId, HttpMethod.POST,
                requestWithCookieAndBody(new RejectRequest("Needs revision")), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(response).get("status")).isEqualTo("REJECTED");

        ContentDraft reloaded = draftRepository.findById(draftId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(DraftStatus.DRAFT);
    }

    @Test
    void reject_blankRemarks_returnsBadRequest() {
        String approvalId = (String) data(submit(draftId)).get("id");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/approval/reject/" + approvalId, HttpMethod.POST,
                requestWithCookieAndBody(new RejectRequest("   ")), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void comment_success_isReflectedInApprovalDetail() {
        String approvalId = (String) data(submit(draftId)).get("id");

        ResponseEntity<Map> commentResponse = restTemplate.exchange(
                "/api/approval/comment/" + approvalId, HttpMethod.POST,
                requestWithCookieAndBody(new AddCommentRequest("Can we make this punchier?")), Map.class);
        assertThat(commentResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> detail = restTemplate.exchange(
                "/api/approval/" + approvalId, HttpMethod.GET, requestWithCookie(), Map.class);
        List<?> comments = (List<?>) data(detail).get("comments");
        assertThat(comments).hasSize(1);
    }

    @Test
    void history_returnsSubmitAndApproveEntries() {
        String approvalId = (String) data(submit(draftId)).get("id");
        restTemplate.exchange("/api/approval/approve/" + approvalId, HttpMethod.POST,
                requestWithCookieAndBody(new ApproveRequest(null)), Map.class);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/approval/history/" + draftId, HttpMethod.GET, requestWithCookie(), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> content = (List<?>) data(response).get("content");
        assertThat(content).hasSize(2);
    }

    @Test
    void pending_listsOnlyPendingByDefault() {
        submit(draftId);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/approval/pending", HttpMethod.GET, requestWithCookie(), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> content = (List<?>) data(response).get("content");
        assertThat(content).hasSize(1);
    }

    @Test
    void pending_invalidStatusFilter_returnsBadRequest() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/approval/pending?status=NOT_REAL", HttpMethod.GET, requestWithCookie(), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<Map> submit(UUID contentId) {
        return restTemplate.exchange("/api/approval/submit", HttpMethod.POST,
                requestWithCookieAndBody(new SubmitApprovalRequest(contentId)), Map.class);
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
