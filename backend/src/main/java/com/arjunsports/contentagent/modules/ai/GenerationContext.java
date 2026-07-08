package com.arjunsports.contentagent.modules.ai;

import lombok.Builder;

import java.util.UUID;

/** All the raw input a generation request can draw on - an uploaded file's metadata plus free-text context. */
@Builder
public record GenerationContext(
        UUID mediaId,
        String mediaFileName,
        String mediaDescription,
        String manualNotes,
        String eventDetails,
        String achievement,
        String competitionResults,
        String trainingSession,
        String coachNotes) {

    public boolean hasAnyInput() {
        return mediaId != null
                || hasText(manualNotes) || hasText(eventDetails) || hasText(achievement)
                || hasText(competitionResults) || hasText(trainingSession) || hasText(coachNotes);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
