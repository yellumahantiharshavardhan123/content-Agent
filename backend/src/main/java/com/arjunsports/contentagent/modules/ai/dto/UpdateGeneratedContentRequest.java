package com.arjunsports.contentagent.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateGeneratedContentRequest(
        @NotBlank(message = "generatedText is required")
        String generatedText,

        boolean draft) {
}
