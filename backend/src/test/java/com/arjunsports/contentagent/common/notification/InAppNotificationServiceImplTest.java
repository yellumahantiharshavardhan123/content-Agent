package com.arjunsports.contentagent.common.notification;

import com.arjunsports.contentagent.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InAppNotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    private InAppNotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new InAppNotificationServiceImpl(notificationRepository);
    }

    @Test
    void notify_savesNotificationWithGivenFields() {
        UUID recipientId = UUID.randomUUID();
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = notificationService.notify(
                recipientId, NotificationType.PUBLISH_SUCCESS, "Published", "Post went live", "/history/1");

        assertThat(result.getRecipientId()).isEqualTo(recipientId);
        assertThat(result.getType()).isEqualTo(NotificationType.PUBLISH_SUCCESS);
        assertThat(result.isRead()).isFalse();
        assertThat(result.getReadAt()).isNull();
    }

    @Test
    void markAsRead_setsReadFlagAndTimestamp() {
        UUID id = UUID.randomUUID();
        Notification existing = Notification.builder()
                .recipientId(UUID.randomUUID())
                .type(NotificationType.APPROVAL_REQUIRED)
                .title("t")
                .message("m")
                .build();
        when(notificationRepository.findById(id)).thenReturn(Optional.of(existing));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.markAsRead(id);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().isRead()).isTrue();
        assertThat(captor.getValue().getReadAt()).isNotNull();
    }

    @Test
    void markAsRead_alreadyRead_doesNotResave() {
        UUID id = UUID.randomUUID();
        Notification existing = Notification.builder().read(true).build();
        when(notificationRepository.findById(id)).thenReturn(Optional.of(existing));

        notificationService.markAsRead(id);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_unknownId_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(notificationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markAllAsRead_marksEveryUnreadNotificationForRecipient() {
        UUID recipientId = UUID.randomUUID();
        Notification first = Notification.builder().recipientId(recipientId).build();
        Notification second = Notification.builder().recipientId(recipientId).build();
        Page<Notification> unread = new PageImpl<>(List.of(first, second));
        when(notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(any(), any(Pageable.class)))
                .thenReturn(unread);
        when(notificationRepository.saveAll(any())).thenReturn(List.of(first, second));

        notificationService.markAllAsRead(recipientId);

        assertThat(first.isRead()).isTrue();
        assertThat(second.isRead()).isTrue();
        assertThat(first.getReadAt()).isNotNull();
        verify(notificationRepository).saveAll(unread);
    }

    @Test
    void countUnread_delegatesToRepository() {
        UUID recipientId = UUID.randomUUID();
        when(notificationRepository.countByRecipientIdAndReadFalse(recipientId)).thenReturn(3L);

        assertThat(notificationService.countUnread(recipientId)).isEqualTo(3L);
        verify(notificationRepository, times(1)).countByRecipientIdAndReadFalse(recipientId);
    }
}
