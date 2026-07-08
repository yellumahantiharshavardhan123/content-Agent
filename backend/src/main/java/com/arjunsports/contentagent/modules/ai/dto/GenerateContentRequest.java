package com.arjunsports.contentagent.modules.ai.dto;

import com.arjunsports.contentagent.modules.ai.ContentType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GenerateContentRequest(
        UUID mediaId,

        @NotNull(message = "Content type is required")
        ContentType contentType,

        String manualNotes,
        String eventDetails,
        String achievement,
        String competitionResults,
        String trainingSession,
        String coachNotes) {
}
