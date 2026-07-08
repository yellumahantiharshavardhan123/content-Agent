package com.arjunsports.contentagent.modules.instagram.dto;

import com.arjunsports.contentagent.modules.instagram.entity.InstagramPostStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record InstagramPostResponse(
        UUID id,
        UUID approvalId,
        UUID instagramAccountId,
        UUID mediaId,
        String caption,
        String hashtags,
        InstagramPostStatus status,
        String instagramMediaId,
        String permalink,
        String publisherName,
        String errorMessage,
        Instant publishedAt,
        Instant createdAt,
        UUID createdBy) {
}
