package com.arjunsports.contentagent.modules.instagram.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only timeline of every connect/disconnect/publish/retry action -
 * powers GET /api/instagram/history. Immutable once written, like
 * {@code ApprovalHistory}, so it has no updatedAt/updatedBy columns.
 */
@Getter
@Setter
@Entity
@Table(name = "instagram_publish_history")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstagramPublishHistory {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "instagram_post_id")
    private UUID instagramPostId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private InstagramHistoryAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private InstagramPostStatus status;

    @Column(name = "publisher_name", length = 40)
    private String publisherName;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_email")
    private String actorEmail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
