package com.arjunsports.contentagent.modules.ai.dto;

import com.arjunsports.contentagent.modules.ai.ContentType;
import com.arjunsports.contentagent.modules.ai.GenerationAction;
import com.arjunsports.contentagent.modules.ai.GenerationHistory;
import com.arjunsports.contentagent.modules.ai.GenerationStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record GenerationHistoryResponse(
        UUID id,
        UUID generatedContentId,
        UUID mediaId,
        ContentType contentType,
        GenerationAction action,
        String aiModel,
        GenerationStatus status,
        String errorMessage,
        Integer latencyMs,
        String actorEmail,
        Instant createdAt) {

    public static GenerationHistoryResponse from(GenerationHistory history) {
        return GenerationHistoryResponse.builder()
                .id(history.getId())
                .generatedContentId(history.getGeneratedContentId())
                .mediaId(history.getMediaId())
                .contentType(history.getContentType())
                .action(history.getAction())
                .aiModel(history.getAiModel())
                .status(history.getStatus())
                .errorMessage(history.getErrorMessage())
                .latencyMs(history.getLatencyMs())
                .actorEmail(history.getActorEmail())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
