package com.arjunsports.contentagent.common.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Dispatch gateway for admin-facing notifications. The current
 * implementation persists in-app notifications; additional channels
 * (email, WhatsApp) can be added as further {@link NotificationService}
 * implementations without any change to callers.
 */
public interface NotificationService {

    Notification notify(UUID recipientId, NotificationType type, String title, String message, String link);

    void markAsRead(UUID notificationId);

    void markAllAsRead(UUID recipientId);

    Page<Notification> findForRecipient(UUID recipientId, boolean unreadOnly, Pageable pageable);

    long countUnread(UUID recipientId);
}
