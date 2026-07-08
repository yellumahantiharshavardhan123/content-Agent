package com.arjunsports.contentagent.modules.instagram;

import com.arjunsports.contentagent.common.audit.ActivityAction;
import com.arjunsports.contentagent.common.audit.AuditLogService;
import com.arjunsports.contentagent.common.exception.BadRequestException;
import com.arjunsports.contentagent.common.exception.ConflictException;
import com.arjunsports.contentagent.common.exception.ResourceNotFoundException;
import com.arjunsports.contentagent.common.notification.NotificationService;
import com.arjunsports.contentagent.common.notification.NotificationType;
import com.arjunsports.contentagent.common.security.AuthenticatedActor;
import com.arjunsports.contentagent.common.util.CredentialEncryptionUtil;
import com.arjunsports.contentagent.modules.ai.ContentType;
import com.arjunsports.contentagent.modules.approval.entity.Approval;
import com.arjunsports.contentagent.modules.approval.entity.ApprovalStatus;
import com.arjunsports.contentagent.modules.approval.repository.ApprovalRepository;
import com.arjunsports.contentagent.modules.instagram.dto.ConnectAccountRequest;
import com.arjunsports.contentagent.modules.instagram.dto.InstagramAccountResponse;
import com.arjunsports.contentagent.modules.instagram.dto.InstagramPostResponse;
import com.arjunsports.contentagent.modules.instagram.dto.PublishRequest;
import com.arjunsports.contentagent.modules.instagram.entity.InstagramAccount;
import com.arjunsports.contentagent.modules.instagram.entity.InstagramPost;
import com.arjunsports.contentagent.modules.instagram.entity.InstagramPostStatus;
import com.arjunsports.contentagent.modules.instagram.mapper.InstagramMapper;
import com.arjunsports.contentagent.modules.instagram.provider.InstagramAccountInfo;
import com.arjunsports.contentagent.modules.instagram.provider.InstagramPublishResult;
import com.arjunsports.contentagent.modules.instagram.provider.InstagramPublisher;
import com.arjunsports.contentagent.modules.instagram.provider.InstagramPublisherException;
import com.arjunsports.contentagent.modules.instagram.provider.PublisherResolver;
import com.arjunsports.contentagent.modules.instagram.repository.InstagramAccountRepository;
import com.arjunsports.contentagent.modules.instagram.repository.InstagramPostRepository;
import com.arjunsports.contentagent.modules.instagram.repository.InstagramPublishHistoryRepository;
import com.arjunsports.contentagent.modules.instagram.service.impl.InstagramServiceImpl;
import com.arjunsports.contentagent.modules.instagram.service.impl.PublishTransactionHelper;
import com.arjunsports.contentagent.modules.instagram.validation.InstagramValidator;
import com.arjunsports.contentagent.modules.media.Media;
import com.arjunsports.contentagent.modules.media.MediaRepository;
import com.arjunsports.contentagent.modules.media.MediaType;
import com.arjunsports.contentagent.storage.StorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstagramServiceImplTest {

    @Mock
    private InstagramAccountRepository accountRepository;
    @Mock
    private InstagramPostRepository postRepository;
    @Mock
    private InstagramPublishHistoryRepository historyRepository;
    @Mock
    private ApprovalRepository approvalRepository;
    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private PublisherResolver publisherResolver;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private PublishTransactionHelper publishTransactionHelper;
    @Mock
    private InstagramPublisher publisher;

    private final CredentialEncryptionUtil credentialEncryptionUtil =
            new CredentialEncryptionUtil(Base64.getEncoder().encodeToString(new byte[32]));
    private final InstagramValidator instagramValidator = new InstagramValidator();
    private final InstagramMapper instagramMapper = new InstagramMapper();

    private InstagramServiceImpl service;
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new InstagramServiceImpl(
                accountRepository, postRepository, historyRepository, approvalRepository, mediaRepository,
                storageService, publisherResolver, credentialEncryptionUtil, instagramValidator, instagramMapper,
                auditLogService, notificationService, publishTransactionHelper);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new TestActor(USER_ID), null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void connect_alreadyConnected_throwsConflict() {
        when(accountRepository.findByActiveTrue()).thenReturn(Optional.of(activeAccount()));

        assertThatThrownBy(() -> service.connect(new ConnectAccountRequest("123", null, "token")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void connect_success_encryptsTokenAndSaves() {
        when(accountRepository.findByActiveTrue()).thenReturn(Optional.empty());
        when(publisherResolver.resolve()).thenReturn(publisher);
        when(publisher.verifyAccount("123", "real-token"))
                .thenReturn(InstagramAccountInfo.builder().businessAccountId("123").username("academy_official").build());
        when(accountRepository.save(any(InstagramAccount.class))).thenAnswer(inv -> {
            InstagramAccount a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        InstagramAccountResponse response = service.connect(new ConnectAccountRequest("123", "page-1", "real-token"));

        assertThat(response.connected()).isTrue();
        assertThat(response.username()).isEqualTo("academy_official");
        verify(auditLogService).record(eq(ActivityAction.CONNECT),
                eq("InstagramAccount"), any());
    }

    @Test
    void getStatus_noAccount_returnsNotConnected() {
        when(accountRepository.findByActiveTrue()).thenReturn(Optional.empty());

        InstagramAccountResponse response = service.getStatus();

        assertThat(response.connected()).isFalse();
    }

    @Test
    void publish_noAccountConnected_throwsConflict() {
        when(accountRepository.findByActiveTrue()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publish(publishRequest()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void publish_approvalNotReadyForPublish_throwsBadRequest() {
        when(accountRepository.findByActiveTrue()).thenReturn(Optional.of(activeAccount()));
        Approval approval = approval(ApprovalStatus.PENDING_APPROVAL);
        when(approvalRepository.findById(approval.getId())).thenReturn(Optional.of(approval));

        assertThatThrownBy(() -> service.publish(new PublishRequest(approval.getId(), UUID.randomUUID(), "cap", null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void publish_unknownApproval_throwsResourceNotFound() {
        when(accountRepository.findByActiveTrue()).thenReturn(Optional.of(activeAccount()));
        UUID approvalId = UUID.randomUUID();
        when(approvalRepository.findById(approvalId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publish(new PublishRequest(approvalId, UUID.randomUUID(), "cap", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void publish_alreadyPublished_throwsConflict() {
        InstagramAccount account = activeAccount();
        when(accountRepository.findByActiveTrue()).thenReturn(Optional.of(account));
        Approval approval = approval(ApprovalStatus.READY_FOR_PUBLISH);
        when(approvalRepository.findById(approval.getId())).thenReturn(Optional.of(approval));
        Media media = media();
        when(mediaRepository.findByIdAndDeletedFalse(media.getId())).thenReturn(Optional.of(media));
        when(postRepository.existsByApprovalIdAndStatus(approval.getId(), InstagramPostStatus.FAILED)).thenReturn(false);
        when(postRepository.existsByApprovalIdAndStatus(approval.getId(), InstagramPostStatus.PUBLISHED)).thenReturn(true);

        assertThatThrownBy(() -> service.publish(new PublishRequest(approval.getId(), media.getId(), "cap", null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void publish_success_savesPublishedPostAndNotifies() {
        InstagramAccount account = activeAccount();
        when(accountRepository.findByActiveTrue()).thenReturn(Optional.of(account));
        Approval approval = approval(ApprovalStatus.READY_FOR_PUBLISH);
        when(approvalRepository.findById(approval.getId())).thenReturn(Optional.of(approval));
        Media media = media();
        when(mediaRepository.findByIdAndDeletedFalse(media.getId())).thenReturn(Optional.of(media));
        when(postRepository.existsByApprovalIdAndStatus(any(), any())).thenReturn(false);
        when(storageService.presignedGetUrl(eq(media.getStorageKey()), any(Duration.class)))
                .thenReturn("https://public.example.com/image.jpg");
        when(publisherResolver.resolve()).thenReturn(publisher);
        when(publisher.getPublisherName()).thenReturn("meta-graph-api");
        when(publisher.publish(any())).thenReturn(
                InstagramPublishResult.builder().instagramMediaId("ig_123").permalink("https://instagram.com/p/ig_123/").build());
        when(postRepository.save(any(InstagramPost.class))).thenAnswer(inv -> {
            InstagramPost p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        InstagramPostResponse response = service.publish(new PublishRequest(approval.getId(), media.getId(), "Great day!", "#win"));

        assertThat(response.status()).isEqualTo(InstagramPostStatus.PUBLISHED);
        assertThat(response.instagramMediaId()).isEqualTo("ig_123");
        verify(publishTransactionHelper).notifyStarted(eq(USER_ID), anyString());
        verify(notificationService).notify(eq(USER_ID),
                eq(NotificationType.PUBLISH_SUCCESS), anyString(), anyString(), any());
        verify(publishTransactionHelper, never())
                .recordFailure(any(), any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void publish_providerFailure_recordsFailureAndRethrows() {
        InstagramAccount account = activeAccount();
        when(accountRepository.findByActiveTrue()).thenReturn(Optional.of(account));
        Approval approval = approval(ApprovalStatus.READY_FOR_PUBLISH);
        when(approvalRepository.findById(approval.getId())).thenReturn(Optional.of(approval));
        Media media = media();
        when(mediaRepository.findByIdAndDeletedFalse(media.getId())).thenReturn(Optional.of(media));
        when(postRepository.existsByApprovalIdAndStatus(any(), any())).thenReturn(false);
        when(storageService.presignedGetUrl(eq(media.getStorageKey()), any(Duration.class)))
                .thenReturn("https://public.example.com/image.jpg");
        when(publisherResolver.resolve()).thenReturn(publisher);
        when(publisher.getPublisherName()).thenReturn("meta-graph-api");
        when(publisher.publish(any())).thenThrow(
                new InstagramPublisherException(InstagramPublisherException.Reason.UNAVAILABLE, "down"));

        assertThatThrownBy(() -> service.publish(new PublishRequest(approval.getId(), media.getId(), "cap", null)))
                .isInstanceOf(InstagramPublisherException.class);

        verify(postRepository, never()).save(any());
        verify(publishTransactionHelper).notifyStarted(eq(USER_ID), anyString());
        verify(publishTransactionHelper).recordFailure(eq(approval.getId()), eq(account.getId()), eq(media.getId()),
                eq("cap"), any(), eq("meta-graph-api"), eq("down"), any(), anyString(), eq(false));
    }

    @Test
    void disconnect_noActiveAccount_throwsBadRequest() {
        when(accountRepository.findByActiveTrue()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.disconnect()).isInstanceOf(BadRequestException.class);
    }

    @Test
    void disconnect_success_deactivatesAccount() {
        InstagramAccount account = activeAccount();
        when(accountRepository.findByActiveTrue()).thenReturn(Optional.of(account));
        when(accountRepository.save(any(InstagramAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        service.disconnect();

        assertThat(account.isActive()).isFalse();
        assertThat(account.getDisconnectedAt()).isNotNull();
    }

    private InstagramAccount activeAccount() {
        InstagramAccount account = InstagramAccount.builder()
                .businessAccountId("123")
                .accessTokenEncrypted(credentialEncryptionUtil.encrypt("real-token"))
                .active(true)
                .connectedAt(java.time.Instant.now())
                .build();
        account.setId(UUID.randomUUID());
        return account;
    }

    private Approval approval(ApprovalStatus status) {
        Approval approval = Approval.builder()
                .contentId(UUID.randomUUID())
                .contentTitle("Regional Championship Recap")
                .contentType(ContentType.INSTAGRAM_CAPTION)
                .status(status)
                .build();
        approval.setId(UUID.randomUUID());
        return approval;
    }

    private Media media() {
        Media media = Media.builder()
                .fileName("trophy.jpg")
                .storageKey("media/trophy.jpg")
                .contentType("image/jpeg")
                .mediaType(MediaType.IMAGE)
                .fileSizeBytes(1024L)
                .build();
        media.setId(UUID.randomUUID());
        return media;
    }

    private PublishRequest publishRequest() {
        return new PublishRequest(UUID.randomUUID(), UUID.randomUUID(), "caption", null);
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
