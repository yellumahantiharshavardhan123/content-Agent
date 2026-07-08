package com.arjunsports.contentagent.modules.draft.dto;

import com.arjunsports.contentagent.modules.ai.ContentType;
import com.arjunsports.contentagent.modules.draft.DraftStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record DraftResponse(
        UUID id,
        UUID generatedContentId,
        UUID mediaId,
        ContentType contentType,
        String title,
        String contentText,
        DraftStatus status,
        boolean deleted,
        Instant deletedAt,
        Instant createdAt,
        Instant updatedAt,
        UUID createdBy,
        UUID updatedBy) {
}
