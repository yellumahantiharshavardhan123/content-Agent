package com.arjunsports.contentagent.modules.ai.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegenerateContentRequest(
        @NotNull(message = "contentId is required")
        UUID contentId) {
}
