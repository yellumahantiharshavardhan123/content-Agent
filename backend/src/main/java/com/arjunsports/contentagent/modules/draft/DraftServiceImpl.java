package com.arjunsports.contentagent.modules.draft;

import com.arjunsports.contentagent.common.audit.ActivityAction;
import com.arjunsports.contentagent.common.audit.AuditLogService;
import com.arjunsports.contentagent.common.exception.BadRequestException;
import com.arjunsports.contentagent.common.exception.ConflictException;
import com.arjunsports.contentagent.common.exception.ResourceNotFoundException;
import com.arjunsports.contentagent.common.exception.UnauthorizedException;
import com.arjunsports.contentagent.common.notification.NotificationService;
import com.arjunsports.contentagent.common.notification.NotificationType;
import com.arjunsports.contentagent.common.security.AuthenticatedActor;
import com.arjunsports.contentagent.modules.ai.ContentType;
import com.arjunsports.contentagent.modules.ai.GeneratedContent;
import com.arjunsports.contentagent.modules.ai.GeneratedContentRepository;
import com.arjunsports.contentagent.modules.draft.dto.CreateDraftRequest;
import com.arjunsports.contentagent.modules.draft.dto.DraftResponse;
import com.arjunsports.contentagent.modules.draft.dto.UpdateDraftRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DraftServiceImpl implements DraftService {

    private static final Set<DraftStatus> TERMINAL_FOR_FINALIZE = Set.of(
            DraftStatus.APPROVED, DraftStatus.PUBLISHED, DraftStatus.ARCHIVED);

    private final DraftRepository draftRepository;
    private final GeneratedContentRepository generatedContentRepository;
    private final DraftValidator draftValidator;
    private final DraftMapper draftMapper;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public DraftResponse createDraft(CreateDraftRequest request) {
        AuthenticatedActor actor = requireActor();

        GeneratedContent source = generatedContentRepository.findById(request.generatedContentId())
                .orElseThrow(() -> ResourceNotFoundException.of("GeneratedContent", request.generatedContentId()));
        draftValidator.validateContentText(source.getGeneratedText());

        String title = (request.title() != null && !request.title().isBlank())
                ? request.title().trim()
                : humanize(source.getContentType()) + " Draft";

        ContentDraft draft = ContentDraft.builder()
                .generatedContentId(source.getId())
                .mediaId(source.getMediaId())
                .contentType(source.getContentType())
                .title(title)
                .contentText(source.getGeneratedText())
                .status(DraftStatus.DRAFT)
                .build();
        ContentDraft saved = draftRepository.save(draft);

        auditLogService.record(ActivityAction.CREATE, "ContentDraft", saved.getId(),
                Map.of("contentType", saved.getContentType().name()));
        notificationService.notify(actor.getId(), NotificationType.DRAFT_SAVED,
                "Draft saved", saved.getTitle() + " was saved as a draft", null);

        return draftMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public DraftResponse updateDraft(UUID id, UpdateDraftRequest request) {
        AuthenticatedActor actor = requireActor();
        ContentDraft draft = findActiveOrThrow(id);

        draftValidator.validateContentText(request.contentText());
        DraftStatus requestedStatus = draftValidator.parseStatus(request.status());

        if (requestedStatus == DraftStatus.APPROVED && requestedStatus != draft.getStatus()) {
            guardAgainstDuplicateFinal(draft);
        }

        if (request.title() != null && !request.title().isBlank()) {
            draft.setTitle(request.title().trim());
        }
        draft.setContentText(request.contentText());
        if (requestedStatus != null) {
            draft.setStatus(requestedStatus);
        }

        ContentDraft saved = draftRepository.save(draft);
        auditLogService.record(ActivityAction.UPDATE, "ContentDraft", saved.getId());
        notificationService.notify(actor.getId(), NotificationType.DRAFT_UPDATED,
                "Draft updated", saved.getTitle() + " was updated", null);

        return draftMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteDraft(UUID id) {
        AuthenticatedActor actor = requireActor();
        ContentDraft draft = findAnyOrThrow(id);
        if (draft.isDeleted()) {
            throw new BadRequestException("Draft is already deleted");
        }

        draft.setDeleted(true);
        draft.setDeletedAt(Instant.now());
        draftRepository.save(draft);

        auditLogService.record(ActivityAction.DELETE, "ContentDraft", draft.getId());
        notificationService.notify(actor.getId(), NotificationType.DRAFT_DELETED,
                "Draft deleted", draft.getTitle() + " was deleted", null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DraftResponse> listDrafts(
            String search, DraftStatus status, ContentType contentType, UUID mediaId,
            boolean includeDeleted, Pageable pageable) {
        var spec = DraftSpecifications.build(search, status, contentType, mediaId, includeDeleted);
        return draftRepository.findAll(spec, pageable).map(draftMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public DraftResponse getDraft(UUID id) {
        return draftMapper.toResponse(findAnyOrThrow(id));
    }

    @Override
    @Transactional
    public DraftResponse duplicateDraft(UUID id) {
        ContentDraft source = findAnyOrThrow(id);
        if (source.isDeleted()) {
            throw new BadRequestException("Cannot duplicate a deleted draft. Restore it first.");
        }

        ContentDraft copy = ContentDraft.builder()
                .generatedContentId(source.getGeneratedContentId())
                .mediaId(source.getMediaId())
                .contentType(source.getContentType())
                .title(source.getTitle() + " (Copy)")
                .contentText(source.getContentText())
                .status(DraftStatus.DRAFT)
                .build();
        ContentDraft saved = draftRepository.save(copy);

        auditLogService.record(ActivityAction.CREATE, "ContentDraft", saved.getId(),
                Map.of("duplicatedFrom", source.getId().toString()));

        return draftMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public DraftResponse restoreDraft(UUID id) {
        AuthenticatedActor actor = requireActor();
        ContentDraft draft = findAnyOrThrow(id);
        if (!draft.isDeleted()) {
            throw new BadRequestException("Draft is not deleted");
        }

        draft.setDeleted(false);
        draft.setDeletedAt(null);
        ContentDraft saved = draftRepository.save(draft);

        auditLogService.record(ActivityAction.RESTORE, "ContentDraft", saved.getId());
        notificationService.notify(actor.getId(), NotificationType.DRAFT_RESTORED,
                "Draft restored", saved.getTitle() + " was restored", null);

        return draftMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public DraftResponse finalizeDraft(UUID id) {
        ContentDraft draft = findActiveOrThrow(id);
        if (TERMINAL_FOR_FINALIZE.contains(draft.getStatus())) {
            throw new ConflictException("Draft is already " + draft.getStatus().name().toLowerCase());
        }
        guardAgainstDuplicateFinal(draft);

        draft.setStatus(DraftStatus.APPROVED);
        ContentDraft saved = draftRepository.save(draft);

        auditLogService.record(ActivityAction.APPROVE, "ContentDraft", saved.getId());

        return draftMapper.toResponse(saved);
    }

    private void guardAgainstDuplicateFinal(ContentDraft draft) {
        if (draft.getGeneratedContentId() == null) {
            return;
        }
        boolean anotherFinalExists = draftRepository.existsByGeneratedContentIdAndStatusAndDeletedFalseAndIdNot(
                draft.getGeneratedContentId(), DraftStatus.APPROVED, draft.getId());
        if (anotherFinalExists) {
            throw new ConflictException("A final draft already exists for this generated content");
        }
    }

    private ContentDraft findAnyOrThrow(UUID id) {
        return draftRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("ContentDraft", id));
    }

    private ContentDraft findActiveOrThrow(UUID id) {
        ContentDraft draft = findAnyOrThrow(id);
        if (draft.isDeleted()) {
            throw new BadRequestException("Cannot modify a deleted draft. Restore it first.");
        }
        return draft;
    }

    private String humanize(ContentType contentType) {
        String[] words = contentType.name().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase()).append(' ');
        }
        return sb.toString().trim();
    }

    private AuthenticatedActor requireActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof AuthenticatedActor actor)) {
            throw new UnauthorizedException("No authenticated user in context");
        }
        return actor;
    }
}
