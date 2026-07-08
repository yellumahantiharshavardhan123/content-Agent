package com.arjunsports.contentagent.modules.approval.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** A reviewer/submitter remark on an {@link Approval}. Immutable once posted - part of the review record. */
@Getter
@Setter
@Entity
@Table(name = "approval_comments")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalComment {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "approval_id", nullable = false)
    private UUID approvalId;

    @Column(name = "content_id")
    private UUID contentId;

    @Column(name = "author_id")
    private UUID authorId;

    @Column(name = "author_email")
    private String authorEmail;

    @Column(name = "comment", columnDefinition = "TEXT", nullable = false)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
