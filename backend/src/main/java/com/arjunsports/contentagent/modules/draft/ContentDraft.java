package com.arjunsports.contentagent.modules.draft;

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
 * A curated, review-pipeline copy of a {@link com.arjunsports.contentagent.modules.ai.GeneratedContent}
 * row. Created explicitly ("Save Draft"); its text is independently editable from that point on and no
 * longer tracks the source row's later edits or regenerations.
 */
@Getter
@Setter
@Entity
@Table(name = "content_drafts")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentDraft extends BaseEntity {

    @Column(name = "generated_content_id")
    private UUID generatedContentId;

    @Column(name = "media_id")
    private UUID mediaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 40)
    private ContentType contentType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content_text", columnDefinition = "TEXT", nullable = false)
    private String contentText;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DraftStatus status = DraftStatus.DRAFT;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
