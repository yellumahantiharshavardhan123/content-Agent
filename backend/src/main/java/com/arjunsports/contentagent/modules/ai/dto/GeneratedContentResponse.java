package com.arjunsports.contentagent.modules.ai.dto;

import com.arjunsports.contentagent.modules.ai.ContentType;
import com.arjunsports.contentagent.modules.ai.GeneratedContent;
import com.arjunsports.contentagent.modules.ai.GenerationStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record GeneratedContentResponse(
        UUID id,
        UUID mediaId,
        ContentType contentType,
        UUID promptTemplateId,
        String generatedText,
        String aiModel,
        GenerationStatus status,
        String errorMessage,
        boolean draft,
        boolean edited,
        Instant createdAt,
        Instant updatedAt,
        UUID createdBy) {

    public static GeneratedContentResponse from(GeneratedContent content) {
        return GeneratedContentResponse.builder()
                .id(content.getId())
                .mediaId(content.getMediaId())
                .contentType(content.getContentType())
                .promptTemplateId(content.getPromptTemplateId())
                .generatedText(content.getGeneratedText())
                .aiModel(content.getAiModel())
                .status(content.getStatus())
                .errorMessage(content.getErrorMessage())
                .draft(content.isDraft())
                .edited(content.isEdited())
                .createdAt(content.getCreatedAt())
                .updatedAt(content.getUpdatedAt())
                .createdBy(content.getCreatedBy())
                .build();
    }
}
