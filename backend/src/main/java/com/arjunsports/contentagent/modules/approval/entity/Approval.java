package com.arjunsports.contentagent.modules.approval.entity;

import com.arjunsports.contentagent.common.entity.BaseEntity;
import com.arjunsports.contentagent.modules.ai.ContentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A single review cycle for a {@link com.arjunsports.contentagent.modules.draft.ContentDraft}.
 * Rejecting a draft lets it be resubmitted later, so a draft may accumulate
 * several Approval rows over its lifetime - the current/active one is
 * whichever is not yet REJECTED/READY_FOR_PUBLISH, enforced by a partial
 * unique index (one PENDING_APPROVAL row per content_id at a time).
 * {@code contentTitle}/{@code contentType} are denormalized from the source
 * draft at submission time, the same way {@code ContentDraft} denormalizes
 * from {@code GeneratedContent} - purely for display/search, not a live join.
 */
@Getter
@Setter
@Entity
@Table(name = "approvals")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Approval extends BaseEntity {

    @Column(name = "content_id")
    private UUID contentId;

    @Column(name = "content_title", length = 200)
    private String contentTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", length = 40)
    private ContentType contentType;

    @Column(name = "reviewer_id")
    private UUID reviewerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING_APPROVAL;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;
}
