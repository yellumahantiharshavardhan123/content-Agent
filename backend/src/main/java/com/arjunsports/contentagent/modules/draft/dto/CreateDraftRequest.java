package com.arjunsports.contentagent.modules.draft.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateDraftRequest(
        @NotNull(message = "generatedContentId is required")
        UUID generatedContentId,

        String title) {
}
