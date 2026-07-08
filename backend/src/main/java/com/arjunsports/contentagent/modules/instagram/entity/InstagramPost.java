package com.arjunsports.contentagent.modules.instagram.entity;

import com.arjunsports.contentagent.common.entity.BaseEntity;
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
 * One publish attempt of an approved piece of content to Instagram. Created
 * from (and denormalizing nothing beyond IDs from) an {@link com.arjunsports.contentagent.modules.approval.entity.Approval}
 * in {@code READY_FOR_PUBLISH} status - the caption/hashtags/media are this
 * row's own copy, independently editable up until the moment of publish.
 */
@Getter
@Setter
@Entity
@Table(name = "instagram_posts")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstagramPost extends BaseEntity {

    @Column(name = "approval_id")
    private UUID approvalId;

    @Column(name = "instagram_account_id")
    private UUID instagramAccountId;

    @Column(name = "media_id")
    private UUID mediaId;

    @Column(name = "caption", columnDefinition = "TEXT", nullable = false)
    private String caption;

    @Column(name = "hashtags", columnDefinition = "TEXT")
    private String hashtags;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private InstagramPostStatus status = InstagramPostStatus.PENDING;

    @Column(name = "instagram_media_id")
    private String instagramMediaId;

    @Column(name = "permalink")
    private String permalink;

    @Column(name = "publisher_name", length = 40)
    private String publisherName;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "published_at")
    private Instant publishedAt;
}
