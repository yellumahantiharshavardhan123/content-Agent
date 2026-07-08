package com.arjunsports.contentagent.modules.ai;

import com.arjunsports.contentagent.common.audit.ActivityAction;
import com.arjunsports.contentagent.common.audit.AuditLogService;
import com.arjunsports.contentagent.common.notification.NotificationService;
import com.arjunsports.contentagent.common.notification.NotificationType;
import com.arjunsports.contentagent.common.security.AuthenticatedActor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Records a failed generate/regenerate attempt (history + audit log +
 * notification) in its own transaction. This must be a separate bean: the
 * calling method re-throws the AIProviderException afterwards, and Spring
 * marks a @Transactional method's transaction rollback-only the moment any
 * unchecked exception crosses its boundary - even if the exception was
 * caught internally first. Without REQUIRES_NEW here, these "we failed,
 * here's a record of it" writes would themselves be rolled back by that
 * same rollback, silently erasing the failure from history/audit/notifications.
 */
@Component
@RequiredArgsConstructor
public class GenerationFailureRecorder {

    private final GenerationHistoryRepository generationHistoryRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UUID generatedContentId, UUID mediaId, ContentType contentType, UUID promptTemplateId,
            GenerationAction action, String errorMessage, AuthenticatedActor actor) {
        GenerationHistory history = GenerationHistory.builder()
                .generatedContentId(generatedContentId)
                .mediaId(mediaId)
                .contentType(contentType)
                .promptTemplateId(promptTemplateId)
                .action(action)
                .status(GenerationStatus.FAILED)
                .errorMessage(errorMessage)
                .actorId(actor.getId())
                .actorEmail(actor.getEmail())
                .build();
        generationHistoryRepository.save(history);

        auditLogService.record(ActivityAction.GENERATE, "GeneratedContent", generatedContentId,
                Map.of("contentType", contentType.name(), "status", "FAILED"));

        notificationService.notify(actor.getId(), NotificationType.GENERATION_FAILED,
                "Content generation failed", contentType + " generation failed: " + errorMessage, null);
    }
}
