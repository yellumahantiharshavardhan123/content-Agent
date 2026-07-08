package com.arjunsports.contentagent.modules.ai.dto;

import com.arjunsports.contentagent.modules.ai.ContentType;
import com.arjunsports.contentagent.modules.ai.PromptTemplate;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record PromptTemplateResponse(
        UUID id,
        ContentType contentType,
        String name,
        String description,
        String systemPrompt,
        String userPromptTemplate,
        int version,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public static PromptTemplateResponse from(PromptTemplate template) {
        return PromptTemplateResponse.builder()
                .id(template.getId())
                .contentType(template.getContentType())
                .name(template.getName())
                .description(template.getDescription())
                .systemPrompt(template.getSystemPrompt())
                .userPromptTemplate(template.getUserPromptTemplate())
                .version(template.getVersion())
                .active(template.isActive())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}
