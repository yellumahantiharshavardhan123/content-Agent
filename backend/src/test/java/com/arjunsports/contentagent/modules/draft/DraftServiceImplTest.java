package com.arjunsports.contentagent.modules.draft;

import com.arjunsports.contentagent.common.audit.AuditLogService;
import com.arjunsports.contentagent.common.exception.BadRequestException;
import com.arjunsports.contentagent.common.exception.ConflictException;
import com.arjunsports.contentagent.common.exception.ResourceNotFoundException;
import com.arjunsports.contentagent.common.notification.NotificationService;
import com.arjunsports.contentagent.common.security.AuthenticatedActor;
import com.arjunsports.contentagent.modules.ai.ContentType;
import com.arjunsports.contentagent.modules.ai.GeneratedContent;
import com.arjunsports.contentagent.modules.ai.GeneratedContentRepository;
import com.arjunsports.contentagent.modules.ai.GenerationStatus;
import com.arjunsports.contentagent.modules.draft.dto.CreateDraftRequest;
import com.arjunsports.contentagent.modules.draft.dto.DraftResponse;
import com.arjunsports.contentagent.modules.draft.dto.UpdateDraftRequest;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DraftServiceImplTest {

    @Mock
    private DraftRepository draftRepository;
    @Mock
    private GeneratedContentRepository generatedContentRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private NotificationService notificationService;

    private final DraftValidator draftValidator = new DraftValidator();
    private final DraftMapper draftMapper = new DraftMapper();

    private DraftServiceImpl service;
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new DraftServiceImpl(
                draftRepository, generatedContentRepository, draftValidator, draftMapper,
                auditLogService, notificationService);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new TestActor(USER_ID), null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createDraft_unknownGeneratedContent_throwsResourceNotFound() {
        UUID sourceId = UUID.randomUUID();
        when(generatedContentRepository.findById(sourceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createDraft(new CreateDraftRequest(sourceId, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createDraft_sourceHasNoGeneratedText_throwsBadRequest() {
        UUID sourceId = UUID.randomUUID();
        GeneratedContent source = generatedContent(sourceId, null, GenerationStatus.FAILED);
        when(generatedContentRepository.findById(sourceId)).thenReturn(Optional.of(source));

        assertThatThrownBy(() -> service.createDraft(new CreateDraftRequest(sourceId, null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createDraft_success_copiesTextAndDefaultsTitleAndNotifies() {
        UUID sourceId = UUID.randomUUID();
        GeneratedContent source = generatedContent(sourceId, "Great shot!", GenerationStatus.SUCCESS);
        when(generatedContentRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(draftRepository.save(any(ContentDraft.class))).thenAnswer(inv -> {
            ContentDraft d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        DraftResponse response = service.createDraft(new CreateDraftRequest(sourceId, null));

        assertThat(response.contentText()).isEqualTo("Great shot!");
        assertThat(response.title()).isEqualTo("Instagram Caption Draft");
        assertThat(response.status()).isEqualTo(DraftStatus.DRAFT);
        verify(notificationService).notify(eq(USER_ID), eq(com.arjunsports.contentagent.common.notification.NotificationType.DRAFT_SAVED),
                anyString(), anyString(), any());
    }

    @Test
    void updateDraft_blankContentText_throwsBadRequest() {
        UUID id = UUID.randomUUID();
        ContentDraft draft = activeDraft(id);
        when(draftRepository.findById(id)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.updateDraft(id, new UpdateDraftRequest("Title", "  ", null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateDraft_deletedDraft_throwsBadRequest() {
        UUID id = UUID.randomUUID();
        ContentDraft draft = activeDraft(id);
        draft.setDeleted(true);
        when(draftRepository.findById(id)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.updateDraft(id, new UpdateDraftRequest("Title", "text", null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateDraft_invalidStatus_throwsBadRequest() {
        UUID id = UUID.randomUUID();
        ContentDraft draft = activeDraft(id);
        when(draftRepository.findById(id)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.updateDraft(id, new UpdateDraftRequest("Title", "text", "NOT_A_STATUS")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateDraft_movesBackToDraftState() {
        UUID id = UUID.randomUUID();
        ContentDraft draft = activeDraft(id);
        draft.setStatus(DraftStatus.APPROVED);
        when(draftRepository.findById(id)).thenReturn(Optional.of(draft));
        when(draftRepository.save(any(ContentDraft.class))).thenAnswer(inv -> inv.getArgument(0));

        DraftResponse response = service.updateDraft(id, new UpdateDraftRequest(null, "text", "DRAFT"));

        assertThat(response.status()).isEqualTo(DraftStatus.DRAFT);
    }

    @Test
    void deleteDraft_alreadyDeleted_throwsBadRequest() {
        UUID id = UUID.randomUUID();
        ContentDraft draft = activeDraft(id);
        draft.setDeleted(true);
        when(draftRepository.findById(id)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.deleteDraft(id)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void deleteDraft_success_softDeletesAndNotifies() {
        UUID id = UUID.randomUUID();
        ContentDraft draft = activeDraft(id);
        when(draftRepository.findById(id)).thenReturn(Optional.of(draft));
        when(draftRepository.save(any(ContentDraft.class))).thenAnswer(inv -> inv.getArgument(0));

        service.deleteDraft(id);

        assertThat(draft.isDeleted()).isTrue();
        assertThat(draft.getDeletedAt()).isNotNull();
        verify(notificationService).notify(eq(USER_ID), eq(com.arjunsports.contentagent.common.notification.NotificationType.DRAFT_DELETED),
                anyString(), anyString(), any());
    }

    @Test
    void restoreDraft_notDeleted_throwsBadRequest() {
        UUID id = UUID.randomUUID();
        ContentDraft draft = activeDraft(id);
        when(draftRepository.findById(id)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.restoreDraft(id)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void restoreDraft_success_clearsDeletedFlagAndNotifies() {
        UUID id = UUID.randomUUID();
        ContentDraft draft = activeDraft(id);
        draft.setDeleted(true);
        when(draftRepository.findById(id)).thenReturn(Optional.of(draft));
        when(draftRepository.save(any(ContentDraft.class))).thenAnswer(inv -> inv.getArgument(0));

        service.restoreDraft(id);

        assertThat(draft.isDeleted()).isFalse();
        assertThat(draft.getDeletedAt()).isNull();
        verify(notificationService).notify(eq(USER_ID), eq(com.arjunsports.contentagent.common.notification.NotificationType.DRAFT_RESTORED),
                anyString(), anyString(), any());
    }

    @Test
    void duplicateDraft_deletedSource_throwsBadRequest() {
        UUID id = UUID.randomUUID();
        ContentDraft draft = activeDraft(id);
        draft.setDeleted(true);
        when(draftRepository.findById(id)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.duplicateDraft(id)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void duplicateDraft_success_createsNewDraftResetToDraftStatus() {
        UUID id = UUID.randomUUID();
        ContentDraft draft = activeDraft(id);
        draft.setStatus(DraftStatus.APPROVED);
        when(draftRepository.findById(id)).thenReturn(Optional.of(draft));
        when(draftRepository.save(any(ContentDraft.class))).thenAnswer(inv -> {
            ContentDraft d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        DraftResponse response = service.duplicateDraft(id);

        assertThat(response.id()).isNotEqualTo(id);
        assertThat(response.status()).isEqualTo(DraftStatus.DRAFT);
        assertThat(response.title()).endsWith("(Copy)");
    }

    @Test
    void finalizeDraft_alreadyApproved_throwsConflict() {
        UUID id = UUID.randomUUID();
        ContentDraft draft = activeDraft(id);
        draft.setStatus(DraftStatus.APPROVED);
        when(draftRepository.findById(id)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.finalizeDraft(id)).isInstanceOf(ConflictException.class);
    }

    @Test
    void finalizeDraft_anotherFinalExistsForSameSource_throwsConflict() {
        UUID id = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        ContentDraft draft = activeDraft(id);
        draft.setGeneratedContentId(sourceId);
        when(draftRepository.findById(id)).thenReturn(Optional.of(draft));
        when(draftRepository.existsByGeneratedContentIdAndStatusAndDeletedFalseAndIdNot(sourceId, DraftStatus.APPROVED, id))
                .thenReturn(true);

        assertThatThrownBy(() -> service.finalizeDraft(id)).isInstanceOf(ConflictException.class);
    }

    @Test
    void finalizeDraft_success_setsApprovedStatus() {
        UUID id = UUID.randomUUID();
        ContentDraft draft = activeDraft(id);
        when(draftRepository.findById(id)).thenReturn(Optional.of(draft));
        when(draftRepository.save(any(ContentDraft.class))).thenAnswer(inv -> inv.getArgument(0));

        DraftResponse response = service.finalizeDraft(id);

        assertThat(response.status()).isEqualTo(DraftStatus.APPROVED);
    }

    private GeneratedContent generatedContent(UUID id, String text, GenerationStatus status) {
        GeneratedContent content = GeneratedContent.builder()
                .contentType(ContentType.INSTAGRAM_CAPTION)
                .promptUsed("prompt")
                .generatedText(text)
                .status(status)
                .build();
        content.setId(id);
        return content;
    }

    private ContentDraft activeDraft(UUID id) {
        ContentDraft draft = ContentDraft.builder()
                .contentType(ContentType.INSTAGRAM_CAPTION)
                .title("A draft")
                .contentText("Some text")
                .status(DraftStatus.DRAFT)
                .build();
        draft.setId(id);
        return draft;
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
