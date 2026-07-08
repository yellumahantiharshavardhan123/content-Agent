package com.arjunsports.contentagent.modules.instagram.dto;

import com.arjunsports.contentagent.modules.instagram.entity.InstagramHistoryAction;
import com.arjunsports.contentagent.modules.instagram.entity.InstagramPostStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record InstagramHistoryResponse(
        UUID id,
        UUID instagramPostId,
        InstagramHistoryAction action,
        InstagramPostStatus status,
        String publisherName,
        String errorMessage,
        UUID actorId,
        String actorEmail,
        Instant createdAt) {
}
