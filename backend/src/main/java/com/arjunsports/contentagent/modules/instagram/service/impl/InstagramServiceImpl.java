package com.arjunsports.contentagent.modules.instagram.service.impl;

import com.arjunsports.contentagent.common.audit.ActivityAction;
import com.arjunsports.contentagent.common.audit.AuditLogService;
import com.arjunsports.contentagent.common.exception.BadRequestException;
import com.arjunsports.contentagent.common.exception.ConflictException;
import com.arjunsports.contentagent.common.exception.ResourceNotFoundException;
import com.arjunsports.contentagent.common.exception.UnauthorizedException;
import com.arjunsports.contentagent.common.notification.NotificationService;
import com.arjunsports.contentagent.common.notification.NotificationType;
import com.arjunsports.contentagent.common.security.AuthenticatedActor;
import com.arjunsports.contentagent.common.util.CredentialEncryptionUtil;
import com.arjunsports.contentagent.modules.approval.entity.Approval;
import com.arjunsports.contentagent.modules.approval.repository.ApprovalRepository;
import com.arjunsports.contentagent.modules.instagram.dto.ConnectAccountRequest;
import com.arjunsports.contentagent.modules.instagram.dto.InstagramAccountResponse;
import com.arjunsports.contentagent.modules.instagram.dto.InstagramHistoryResponse;
import com.arjunsports.contentagent.modules.instagram.dto.InstagramPostResponse;
import com.arjunsports.contentagent.modules.instagram.dto.PublishRequest;
import com.arjunsports.contentagent.modules.instagram.entity.InstagramAccount;
import com.arjunsports.contentagent.modules.instagram.entity.InstagramHistoryAction;
import com.arjunsports.contentagent.modules.instagram.entity.InstagramPost;
import com.arjunsports.contentagent.modules.instagram.entity.InstagramPostStatus;
import com.arjunsports.contentagent.modules.instagram.entity.InstagramPublishHistory;
import com.arjunsports.contentagent.modules.instagram.mapper.InstagramMapper;
import com.arjunsports.contentagent.modules.instagram.provider.InstagramAccountInfo;
import com.arjunsports.contentagent.modules.instagram.provider.InstagramPublishRequest;
import com.arjunsports.contentagent.modules.instagram.provider.InstagramPublishResult;
import com.arjunsports.contentagent.modules.instagram.provider.InstagramPublisher;
import com.arjunsports.contentagent.modules.instagram.provider.InstagramPublisherException;
import com.arjunsports.contentagent.modules.instagram.provider.PublisherResolver;
import com.arjunsports.contentagent.modules.instagram.repository.InstagramAccountRepository;
import com.arjunsports.contentagent.modules.instagram.repository.InstagramPostRepository;
import com.arjunsports.contentagent.modules.instagram.repository.InstagramPublishHistoryRepository;
import com.arjunsports.contentagent.modules.instagram.service.InstagramService;
import com.arjunsports.contentagent.modules.instagram.validation.InstagramValidator;
import com.arjunsports.contentagent.modules.media.Media;
import com.arjunsports.contentagent.modules.media.MediaRepository;
import com.arjunsports.contentagent.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InstagramServiceImpl implements InstagramService {

    private static final Duration MEDIA_URL_EXPIRY = Duration.ofMinutes(30);

    private final InstagramAccountRepository accountRepository;
    private final InstagramPostRepository postRepository;
    private final InstagramPublishHistoryRepository historyRepository;
    private final ApprovalRepository approvalRepository;
    private final MediaRepository mediaRepository;
    private final StorageService storageService;
    private final PublisherResolver publisherResolver;
    private final CredentialEncryptionUtil credentialEncryptionUtil;
    private final InstagramValidator instagramValidator;
    private final InstagramMapper instagramMapper;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final PublishTransactionHelper publishTransactionHelper;

    @Override
    @Transactional
    public InstagramAccountResponse connect(ConnectAccountRequest request) {
        AuthenticatedActor actor = requireActor();
        instagramValidator.validateNoActiveConnection(accountRepository.findByActiveTrue().isPresent());

        InstagramAccountInfo info = publisherResolver.resolve()
                .verifyAccount(request.businessAccountId(), request.accessToken());

        InstagramAccount account = InstagramAccount.builder()
                .businessAccountId(info.businessAccountId())
                .facebookPageId(request.facebookPageId())
                .username(info.username())
                .accessTokenEncrypted(credentialEncryptionUtil.encrypt(request.accessToken()))
                .active(true)
                .connectedAt(Instant.now())
                .build();
        InstagramAccount saved = accountRepository.save(account);

        recordHistory(null, InstagramHistoryAction.CONNECT, null, null, null, actor);
        auditLogService.record(ActivityAction.CONNECT, "InstagramAccount", saved.getId());

        return instagramMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public InstagramAccountResponse getStatus() {
        return accountRepository.findByActiveTrue()
                .map(instagramMapper::toResponse)
                .orElseGet(InstagramAccountResponse::notConnected);
    }

    @Override
    @Transactional
    public InstagramPostResponse publish(PublishRequest request) {
        return doPublish(request, publisherResolver.resolve());
    }

    @Override
    @Transactional
    public InstagramPostResponse publishMock(PublishRequest request) {
        return doPublish(request, publisherResolver.resolveMock());
    }

    private InstagramPostResponse doPublish(PublishRequest request, InstagramPublisher publisher) {
        AuthenticatedActor actor = requireActor();

        InstagramAccount account = accountRepository.findByActiveTrue()
                .orElseThrow(() -> new ConflictException("No Instagram account connected. Connect one first."));

        Approval approval = approvalRepository.findById(request.approvalId())
                .orElseThrow(() -> ResourceNotFoundException.of("Approval", request.approvalId()));
        instagramValidator.validateApprovalPublishable(approval);

        Media media = mediaRepository.findByIdAndDeletedFalse(request.mediaId())
                .orElseThrow(() -> ResourceNotFoundException.of("Media", request.mediaId()));

        boolean isRetry = postRepository.existsByApprovalIdAndStatus(approval.getId(), InstagramPostStatus.FAILED);
        instagramValidator.validateNotAlreadyPublished(
                postRepository.existsByApprovalIdAndStatus(approval.getId(), InstagramPostStatus.PUBLISHED));

        String contentTitle = approval.getContentTitle() != null ? approval.getContentTitle() : "your content";
        publishTransactionHelper.notifyStarted(actor.getId(), contentTitle);

        String fullCaption = combineCaption(request.caption(), request.hashtags());
        String imageUrl = storageService.presignedGetUrl(media.getStorageKey(), MEDIA_URL_EXPIRY);
        String decryptedToken = credentialEncryptionUtil.decrypt(account.getAccessTokenEncrypted());

        try {
            InstagramPublishResult result = publisher.publish(InstagramPublishRequest.builder()
                    .businessAccountId(account.getBusinessAccountId())
                    .accessToken(decryptedToken)
                    .imageUrl(imageUrl)
                    .caption(fullCaption)
                    .build());

            InstagramPost post = InstagramPost.builder()
                    .approvalId(approval.getId())
                    .instagramAccountId(account.getId())
                    .mediaId(media.getId())
                    .caption(request.caption())
                    .hashtags(request.hashtags())
                    .status(InstagramPostStatus.PUBLISHED)
                    .instagramMediaId(result.instagramMediaId())
                    .permalink(result.permalink())
                    .publisherName(publisher.getPublisherName())
                    .publishedAt(Instant.now())
                    .build();
            InstagramPost saved = postRepository.save(post);

            recordHistory(saved.getId(), InstagramHistoryAction.PUBLISH_SUCCESS, InstagramPostStatus.PUBLISHED,
                    publisher.getPublisherName(), null, actor);
            auditLogService.record(isRetry ? ActivityAction.RETRY : ActivityAction.PUBLISH, "InstagramPost", saved.getId());
            notificationService.notify(actor.getId(), NotificationType.PUBLISH_SUCCESS, "Published to Instagram",
                    "\"" + contentTitle + "\" was published successfully.", saved.getPermalink());

            return instagramMapper.toResponse(saved);

        } catch (InstagramPublisherException e) {
            publishTransactionHelper.recordFailure(approval.getId(), account.getId(), media.getId(), request.caption(),
                    request.hashtags(), publisher.getPublisherName(), e.getMessage(), actor, contentTitle, isRetry);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstagramHistoryResponse> getHistory(Pageable pageable) {
        return historyRepository.findAllByOrderByCreatedAtDesc(pageable).map(instagramMapper::toResponse);
    }

    @Override
    @Transactional
    public void disconnect() {
        AuthenticatedActor actor = requireActor();
        InstagramAccount account = accountRepository.findByActiveTrue()
                .orElseThrow(() -> new BadRequestException("No Instagram account is currently connected"));

        account.setActive(false);
        account.setDisconnectedAt(Instant.now());
        accountRepository.save(account);

        recordHistory(null, InstagramHistoryAction.DISCONNECT, null, null, null, actor);
        auditLogService.record(ActivityAction.DISCONNECT, "InstagramAccount", account.getId());
    }

    private String combineCaption(String caption, String hashtags) {
        if (hashtags == null || hashtags.isBlank()) {
            return caption;
        }
        return caption + "\n\n" + hashtags;
    }

    private void recordHistory(UUID instagramPostId, InstagramHistoryAction action,
            InstagramPostStatus status, String publisherName, String errorMessage, AuthenticatedActor actor) {
        InstagramPublishHistory history = InstagramPublishHistory.builder()
                .instagramPostId(instagramPostId)
                .action(action)
                .status(status)
                .publisherName(publisherName)
                .errorMessage(errorMessage)
                .actorId(actor.getId())
                .actorEmail(actor.getEmail())
                .build();
        historyRepository.save(history);
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
