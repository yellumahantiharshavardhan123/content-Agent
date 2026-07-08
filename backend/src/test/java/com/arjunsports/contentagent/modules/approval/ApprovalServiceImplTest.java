package com.arjunsports.contentagent.modules.approval;

import com.arjunsports.contentagent.common.audit.AuditLogService;
import com.arjunsports.contentagent.common.exception.BadRequestException;
import com.arjunsports.contentagent.common.exception.ConflictException;
import com.arjunsports.contentagent.common.exception.ResourceNotFoundException;
import com.arjunsports.contentagent.common.exception.UnauthorizedException;
import com.arjunsports.contentagent.common.notification.NotificationService;
import com.arjunsports.contentagent.common.security.AuthenticatedActor;
import com.arjunsports.contentagent.modules.ai.ContentType;
import com.arjunsports.contentagent.modules.approval.dto.AddCommentRequest;
import com.arjunsports.contentagent.modules.approval.dto.ApproveRequest;
import com.arjunsports.contentagent.modules.approval.dto.ApprovalResponse;
import com.arjunsports.contentagent.modules.approval.dto.RejectRequest;
import com.arjunsports.contentagent.modules.approval.dto.SubmitApprovalRequest;
import com.arjunsports.contentagent.modules.approval.entity.Approval;
import com.arjunsports.contentagent.modules.approval.entity.ApprovalStatus;
import com.arjunsports.contentagent.modules.approval.mapper.ApprovalMapper;
import com.arjunsports.contentagent.modules.approval.repository.ApprovalCommentRepository;
import com.arjunsports.contentagent.modules.approval.repository.ApprovalHistoryRepository;
import com.arjunsports.contentagent.modules.approval.repository.ApprovalRepository;
import com.arjunsports.contentagent.modules.approval.service.impl.ApprovalServiceImpl;
import com.arjunsports.contentagent.modules.approval.validation.ApprovalValidator;
import com.arjunsports.contentagent.modules.draft.ContentDraft;
import com.arjunsports.contentagent.modules.draft.DraftRepository;
import com.arjunsports.contentagent.modules.draft.DraftStatus;
import com.arjunsports.contentagent.modules.user.Role;
import com.arjunsports.contentagent.modules.user.User;
import com.arjunsports.contentagent.modules.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceImplTest {

    @Mock
    private ApprovalRepository approvalRepository;
    @Mock
    private ApprovalHistoryRepository approvalHistoryRepository;
    @Mock
    private ApprovalCommentRepository approvalCommentRepository;
    @Mock
    private DraftRepository draftRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private NotificationService notificationService;

    private final ApprovalValidator approvalValidator = new ApprovalValidator();
    private final ApprovalMapper approvalMapper = new ApprovalMapper();

    private ApprovalServiceImpl service;
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ApprovalServiceImpl(
                approvalRepository, approvalHistoryRepository, approvalCommentRepository, draftRepository,
                userRepository, approvalValidator, approvalMapper, auditLogService, notificationService);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new TestActor(USER_ID), null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submit_withoutAuthentication_throwsUnauthorized() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> service.submit(new SubmitApprovalRequest(UUID.randomUUID())))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void submit_unknownDraft_throwsResourceNotFound() {
        UUID draftId = UUID.randomUUID();
        when(draftRepository.findById(draftId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(new SubmitApprovalRequest(draftId)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void submit_deletedDraft_throwsBadRequest() {
        ContentDraft draft = draft(DraftStatus.DRAFT);
        draft.setDeleted(true);
        when(draftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.submit(new SubmitApprovalRequest(draft.getId())))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void submit_alreadyPending_throwsConflict() {
        ContentDraft draft = draft(DraftStatus.DRAFT);
        when(draftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
        when(approvalRepository.findByContentIdAndStatus(draft.getId(), ApprovalStatus.PENDING_APPROVAL))
                .thenReturn(Optional.of(Approval.builder().build()));

        assertThatThrownBy(() -> service.submit(new SubmitApprovalRequest(draft.getId())))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void submit_success_createsApprovalAndUpdatesDraftStatusAndNotifiesAdmins() {
        ContentDraft draft = draft(DraftStatus.DRAFT);
        when(draftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
        when(approvalRepository.findByContentIdAndStatus(any(), any())).thenReturn(Optional.empty());
        when(approvalRepository.save(any(Approval.class))).thenAnswer(inv -> {
            Approval a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });
        User admin = User.builder().firstName("A").lastName("B").email("admin@test.com")
                .password("x").role(Role.ADMIN).active(true).build();
        admin.setId(UUID.randomUUID());
        when(userRepository.findByRoleAndActiveTrue(Role.ADMIN)).thenReturn(List.of(admin));

        ApprovalResponse response = service.submit(new SubmitApprovalRequest(draft.getId()));

        assertThat(response.status()).isEqualTo(ApprovalStatus.PENDING_APPROVAL);
        assertThat(draft.getStatus()).isEqualTo(DraftStatus.READY_FOR_REVIEW);
        verify(approvalHistoryRepository).save(any());
        verify(notificationService).notify(eq(admin.getId()), any(), any(), any(), any());
    }

    @Test
    void approve_notPending_throwsConflict() {
        Approval approval = approval(ApprovalStatus.REJECTED);
        when(approvalRepository.findById(approval.getId())).thenReturn(Optional.of(approval));

        assertThatThrownBy(() -> service.approve(approval.getId(), new ApproveRequest(null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void approve_draftDeletedSinceSubmission_throwsBadRequest() {
        Approval approval = approval(ApprovalStatus.PENDING_APPROVAL);
        when(approvalRepository.findById(approval.getId())).thenReturn(Optional.of(approval));
        ContentDraft draft = draft(DraftStatus.READY_FOR_REVIEW);
        draft.setDeleted(true);
        when(draftRepository.findById(approval.getContentId())).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.approve(approval.getId(), new ApproveRequest(null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void approve_success_setsReadyForPublishAndApprovesDraftAndNotifiesSubmitter() {
        Approval approval = approval(ApprovalStatus.PENDING_APPROVAL);
        UUID submitterId = UUID.randomUUID();
        approval.setCreatedBy(submitterId);
        when(approvalRepository.findById(approval.getId())).thenReturn(Optional.of(approval));
        ContentDraft draft = draft(DraftStatus.READY_FOR_REVIEW);
        when(draftRepository.findById(approval.getContentId())).thenReturn(Optional.of(draft));
        when(approvalRepository.save(any(Approval.class))).thenAnswer(inv -> inv.getArgument(0));
        when(approvalCommentRepository.findByApprovalIdOrderByCreatedAtAsc(approval.getId())).thenReturn(List.of());

        ApprovalResponse response = service.approve(approval.getId(), new ApproveRequest("Looks great"));

        assertThat(response.status()).isEqualTo(ApprovalStatus.READY_FOR_PUBLISH);
        assertThat(draft.getStatus()).isEqualTo(DraftStatus.APPROVED);
        verify(notificationService).notify(eq(submitterId), any(), any(), any(), any());
    }

    @Test
    void reject_notPending_throwsConflict() {
        Approval approval = approval(ApprovalStatus.READY_FOR_PUBLISH);
        when(approvalRepository.findById(approval.getId())).thenReturn(Optional.of(approval));

        assertThatThrownBy(() -> service.reject(approval.getId(), new RejectRequest("not good enough")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void reject_success_returnsDraftToDraftStatus() {
        Approval approval = approval(ApprovalStatus.PENDING_APPROVAL);
        when(approvalRepository.findById(approval.getId())).thenReturn(Optional.of(approval));
        ContentDraft draft = draft(DraftStatus.READY_FOR_REVIEW);
        when(draftRepository.findById(approval.getContentId())).thenReturn(Optional.of(draft));
        when(approvalRepository.save(any(Approval.class))).thenAnswer(inv -> inv.getArgument(0));
        when(approvalCommentRepository.findByApprovalIdOrderByCreatedAtAsc(approval.getId())).thenReturn(List.of());

        ApprovalResponse response = service.reject(approval.getId(), new RejectRequest("Needs more work"));

        assertThat(response.status()).isEqualTo(ApprovalStatus.REJECTED);
        assertThat(response.remarks()).isEqualTo("Needs more work");
        assertThat(draft.getStatus()).isEqualTo(DraftStatus.DRAFT);
    }

    @Test
    void addComment_success_recordsHistoryAndReturnsComment() {
        Approval approval = approval(ApprovalStatus.PENDING_APPROVAL);
        approval.setCreatedBy(UUID.randomUUID());
        when(approvalRepository.findById(approval.getId())).thenReturn(Optional.of(approval));
        when(approvalCommentRepository.save(any())).thenAnswer(inv -> {
            var c = inv.getArgument(0, com.arjunsports.contentagent.modules.approval.entity.ApprovalComment.class);
            c.setId(UUID.randomUUID());
            return c;
        });

        var response = service.addComment(approval.getId(), new AddCommentRequest("Please fix the CTA"));

        assertThat(response.comment()).isEqualTo("Please fix the CTA");
        verify(approvalHistoryRepository).save(any());
    }

    @Test
    void getById_unknownId_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(approvalRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    private ContentDraft draft(DraftStatus status) {
        ContentDraft draft = ContentDraft.builder()
                .contentType(ContentType.INSTAGRAM_CAPTION)
                .title("A draft")
                .contentText("Some text")
                .status(status)
                .build();
        draft.setId(UUID.randomUUID());
        return draft;
    }

    private Approval approval(ApprovalStatus status) {
        Approval approval = Approval.builder()
                .contentId(UUID.randomUUID())
                .contentTitle("A draft")
                .contentType(ContentType.INSTAGRAM_CAPTION)
                .status(status)
                .build();
        approval.setId(UUID.randomUUID());
        return approval;
    }

    private record TestActor(UUID id) implements AuthenticatedActor {
        @Override
        public UUID getId() {
            return id;
        }

        @Override
        public String getEmail() {
            return "actor@test.com";
        }
    }
}
