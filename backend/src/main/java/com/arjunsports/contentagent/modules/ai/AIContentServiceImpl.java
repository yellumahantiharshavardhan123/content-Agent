package com.arjunsports.contentagent.modules.ai;

import com.arjunsports.contentagent.common.audit.ActivityAction;
import com.arjunsports.contentagent.common.audit.AuditLogService;
import com.arjunsports.contentagent.common.exception.BadRequestException;
import com.arjunsports.contentagent.common.exception.RateLimitExceededException;
import com.arjunsports.contentagent.common.exception.ResourceNotFoundException;
import com.arjunsports.contentagent.common.exception.UnauthorizedException;
import com.arjunsports.contentagent.common.notification.NotificationService;
import com.arjunsports.contentagent.common.notification.NotificationType;
import com.arjunsports.contentagent.common.security.AuthenticatedActor;
import com.arjunsports.contentagent.modules.ai.dto.GenerateContentRequest;
import com.arjunsports.contentagent.modules.ai.dto.GeneratedContentResponse;
import com.arjunsports.contentagent.modules.ai.dto.GenerationHistoryResponse;
import com.arjunsports.contentagent.modules.ai.dto.UpdateGeneratedContentRequest;
import com.arjunsports.contentagent.modules.ai.provider.AIProviderException;
import com.arjunsports.contentagent.modules.media.Media;
import com.arjunsports.contentagent.modules.media.MediaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AIContentServiceImpl implements AIContentService {

    private final PromptRepository promptRepository;
    private final GeneratedContentRepository generatedContentRepository;
    private final GenerationHistoryRepository generationHistoryRepository;
    private final MediaRepository mediaRepository;
    private final PromptValidator promptValidator;
    private final PromptBuilder promptBuilder;
    private final ContentGenerator contentGenerator;
    private final GenerationRateLimiter rateLimiter;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final GenerationFailureRecorder failureRecorder;

    @Override
    @Transactional
    public GeneratedContentResponse generate(GenerateContentRequest request) {
        AuthenticatedActor actor = requireActor();
        if (!rateLimiter.tryConsume(actor.getId())) {
            throw new RateLimitExceededException("Generation rate limit exceeded. Please wait a minute and try again.");
        }

        Media media = resolveMedia(request.mediaId());
        GenerationContext context = GenerationContext.builder()
                .mediaId(request.mediaId())
                .mediaFileName(media != null ? media.getFileName() : null)
                .mediaDescription(media != null ? media.getDescription() : null)
                .manualNotes(request.manualNotes())
                .eventDetails(request.eventDetails())
                .achievement(request.achievement())
                .competitionResults(request.competitionResults())
                .trainingSession(request.trainingSession())
                .coachNotes(request.coachNotes())
                .build();
        promptValidator.validateContext(context);

        PromptTemplate template = promptRepository.findByContentTypeAndActiveTrue(request.contentType()).orElse(null);
        promptValidator.validateTemplate(template, request.contentType());

        PromptBuilder.BuiltPrompt prompt = promptBuilder.build(template, context);

        try {
            ContentGenerator.GenerationOutcome outcome =
                    contentGenerator.generate(prompt.systemPrompt(), prompt.userPrompt());

            GeneratedContent content = GeneratedContent.builder()
                    .mediaId(request.mediaId())
                    .contentType(request.contentType())
                    .promptTemplateId(template.getId())
                    .promptUsed(prompt.userPrompt())
                    .generatedText(outcome.generatedText())
                    .aiModel(outcome.aiModel())
                    .status(GenerationStatus.SUCCESS)
                    .build();
            GeneratedContent saved = generatedContentRepository.save(content);

            recordHistory(saved.getId(), request.mediaId(), request.contentType(), template.getId(),
                    GenerationAction.GENERATE, outcome.aiModel(), GenerationStatus.SUCCESS, null,
                    outcome.latencyMs(), actor);

            auditLogService.record(ActivityAction.GENERATE, "GeneratedContent", saved.getId(),
                    Map.of("contentType", request.contentType().name(), "status", "SUCCESS"));
            notificationService.notify(actor.getId(), NotificationType.GENERATION_COMPLETED,
                    "Content generated", request.contentType() + " generated successfully", null);

            return GeneratedContentResponse.from(saved);

        } catch (AIProviderException e) {
            failureRecorder.recordFailure(null, request.mediaId(), request.contentType(), template.getId(),
                    GenerationAction.GENERATE, e.getMessage(), actor);
            throw e;
        }
    }

    @Override
    @Transactional
    public GeneratedContentResponse regenerate(UUID contentId) {
        AuthenticatedActor actor = requireActor();
        if (!rateLimiter.tryConsume(actor.getId())) {
            throw new RateLimitExceededException("Generation rate limit exceeded. Please wait a minute and try again.");
        }

        GeneratedContent content = generatedContentRepository.findById(contentId)
                .orElseThrow(() -> ResourceNotFoundException.of("GeneratedContent", contentId));

        if (content.getPromptTemplateId() == null) {
            throw new BadRequestException("This content has no associated prompt template to regenerate from");
        }
        PromptTemplate template = promptRepository.findById(content.getPromptTemplateId())
                .orElseThrow(() -> ResourceNotFoundException.of("PromptTemplate", content.getPromptTemplateId()));

        try {
            ContentGenerator.GenerationOutcome outcome =
                    contentGenerator.generate(template.getSystemPrompt(), content.getPromptUsed());

            content.setGeneratedText(outcome.generatedText());
            content.setAiModel(outcome.aiModel());
            content.setStatus(GenerationStatus.SUCCESS);
            content.setErrorMessage(null);
            content.setEdited(false);
            GeneratedContent saved = generatedContentRepository.save(content);

            recordHistory(saved.getId(), saved.getMediaId(), saved.getContentType(), template.getId(),
                    GenerationAction.REGENERATE, outcome.aiModel(), GenerationStatus.SUCCESS, null,
                    outcome.latencyMs(), actor);

            auditLogService.record(ActivityAction.GENERATE, "GeneratedContent", saved.getId(),
                    Map.of("contentType", saved.getContentType().name(), "action", "REGENERATE"));
            notificationService.notify(actor.getId(), NotificationType.GENERATION_COMPLETED,
                    "Content regenerated", saved.getContentType() + " regenerated successfully", null);

            return GeneratedContentResponse.from(saved);

        } catch (AIProviderException e) {
            failureRecorder.recordFailure(content.getId(), content.getMediaId(), content.getContentType(),
                    template.getId(), GenerationAction.REGENERATE, e.getMessage(), actor);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GenerationHistoryResponse> getHistory(Pageable pageable) {
        return generationHistoryRepository.findAllByOrderByCreatedAtDesc(pageable).map(GenerationHistoryResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GeneratedContentResponse> listContent(UUID mediaId, ContentType contentType, Pageable pageable) {
        Page<GeneratedContent> page;
        if (mediaId != null && contentType != null) {
            page = generatedContentRepository.findByMediaIdAndContentType(mediaId, contentType, pageable);
        } else if (mediaId != null) {
            page = generatedContentRepository.findByMediaId(mediaId, pageable);
        } else if (contentType != null) {
            page = generatedContentRepository.findByContentType(contentType, pageable);
        } else {
            page = generatedContentRepository.findAll(pageable);
        }
        return page.map(GeneratedContentResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public GeneratedContentResponse getContent(UUID id) {
        return GeneratedContentResponse.from(findContentOrThrow(id));
    }

    @Override
    @Transactional
    public GeneratedContentResponse updateContent(UUID id, UpdateGeneratedContentRequest request) {
        GeneratedContent content = findContentOrThrow(id);
        boolean textChanged = !request.generatedText().equals(content.getGeneratedText());

        content.setGeneratedText(request.generatedText());
        content.setDraft(request.draft());
        if (textChanged) {
            content.setEdited(true);
        }

        GeneratedContent saved = generatedContentRepository.save(content);
        auditLogService.record(ActivityAction.UPDATE, "GeneratedContent", saved.getId());
        return GeneratedContentResponse.from(saved);
    }

    @Override
    @Transactional
    public void deleteContent(UUID id) {
        GeneratedContent content = findContentOrThrow(id);
        generatedContentRepository.delete(content);
        auditLogService.record(ActivityAction.DELETE, "GeneratedContent", id);
    }

    private GeneratedContent findContentOrThrow(UUID id) {
        return generatedContentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("GeneratedContent", id));
    }

    private Media resolveMedia(UUID mediaId) {
        if (mediaId == null) {
            return null;
        }
        return mediaRepository.findByIdAndDeletedFalse(mediaId)
                .orElseThrow(() -> ResourceNotFoundException.of("Media", mediaId));
    }

    private void recordHistory(UUID generatedContentId, UUID mediaId, ContentType contentType, UUID promptTemplateId,
            GenerationAction action, String aiModel, GenerationStatus status, String errorMessage,
            Integer latencyMs, AuthenticatedActor actor) {
        GenerationHistory history = GenerationHistory.builder()
                .generatedContentId(generatedContentId)
                .mediaId(mediaId)
                .contentType(contentType)
                .promptTemplateId(promptTemplateId)
                .action(action)
                .aiModel(aiModel)
                .status(status)
                .errorMessage(errorMessage)
                .latencyMs(latencyMs)
                .actorId(actor.getId())
                .actorEmail(actor.getEmail())
                .build();
        generationHistoryRepository.save(history);
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
