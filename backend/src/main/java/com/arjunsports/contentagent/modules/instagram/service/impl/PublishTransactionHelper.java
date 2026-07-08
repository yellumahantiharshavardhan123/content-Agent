package com.arjunsports.contentagent.modules.instagram.service.impl;

import com.arjunsports.contentagent.common.audit.ActivityAction;
import com.arjunsports.contentagent.common.audit.AuditLogService;
import com.arjunsports.contentagent.common.notification.NotificationService;
import com.arjunsports.contentagent.common.notification.NotificationType;
import com.arjunsports.contentagent.common.security.AuthenticatedActor;
import com.arjunsports.contentagent.modules.instagram.entity.InstagramHistoryAction;
import com.arjunsports.contentagent.modules.instagram.entity.InstagramPost;
import com.arjunsports.contentagent.modules.instagram.entity.InstagramPostStatus;
import com.arjunsports.contentagent.modules.instagram.entity.InstagramPublishHistory;
import com.arjunsports.contentagent.modules.instagram.repository.InstagramPostRepository;
import com.arjunsports.contentagent.modules.instagram.repository.InstagramPublishHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Side effects of {@code InstagramServiceImpl.doPublish()} that must survive
 * regardless of how that method's own transaction ends. Mirrors {@code
 * GenerationFailureRecorder} from Module 3.
 *
 * <p>{@code doPublish()} calls the real publisher *before* ever inserting a
 * row, then re-throws on failure. Two consequences of that shape land here,
 * both via REQUIRES_NEW so a later rollback in the caller's transaction
 * can't erase them:
 * <ul>
 *   <li>{@link #notifyStarted} - fired before the publisher call. If the
 *       publish then fails, the caller's transaction rolls back; without its
 *       own transaction this notification would roll back with it, silently
 *       erasing the one signal that an attempt was even made.</li>
 *   <li>{@link #recordFailure} - the FAILED post row, its history entry, and
 *       the failure notification. Without REQUIRES_NEW, the re-throw that
 *       follows it would roll back this very failure record along with
 *       everything else in the caller's transaction.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class PublishTransactionHelper {

    private final InstagramPostRepository postRepository;
    private final InstagramPublishHistoryRepository historyRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyStarted(UUID actorId, String contentTitle) {
        notificationService.notify(actorId, NotificationType.PUBLISH_STARTED, "Publishing started",
                "Publishing \"" + contentTitle + "\" to Instagram...", null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UUID approvalId, UUID accountId, UUID mediaId, String caption, String hashtags,
            String publisherName, String errorMessage, AuthenticatedActor actor, String contentTitle,
            boolean isRetry) {
        InstagramPost post = InstagramPost.builder()
                .approvalId(approvalId)
                .instagramAccountId(accountId)
                .mediaId(mediaId)
                .caption(caption)
                .hashtags(hashtags)
                .status(InstagramPostStatus.FAILED)
                .publisherName(publisherName)
                .errorMessage(errorMessage)
                .build();
        InstagramPost saved = postRepository.save(post);

        InstagramPublishHistory history = InstagramPublishHistory.builder()
                .instagramPostId(saved.getId())
                .action(InstagramHistoryAction.PUBLISH_FAILURE)
                .status(InstagramPostStatus.FAILED)
                .publisherName(publisherName)
                .errorMessage(errorMessage)
                .actorId(actor.getId())
                .actorEmail(actor.getEmail())
                .build();
        historyRepository.save(history);

        auditLogService.record(isRetry ? ActivityAction.RETRY : ActivityAction.PUBLISH, "InstagramPost", saved.getId(),
                Map.of("status", "FAILED"));
        notificationService.notify(actor.getId(), NotificationType.PUBLISH_FAILURE, "Instagram publish failed",
                "Failed to publish \"" + contentTitle + "\": " + errorMessage, null);
    }
}
