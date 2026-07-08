package com.arjunsports.contentagent.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdatePromptRequest(
        @NotBlank(message = "Name is required")
        String name,

        String description,

        @NotBlank(message = "System prompt is required")
        String systemPrompt,

        @NotBlank(message = "User prompt template is required")
        String userPromptTemplate) {
}
