package com.arjunsports.contentagent.modules.draft.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateDraftRequest(
        String title,

        @NotBlank(message = "contentText is required")
        String contentText,

        /** Raw status name, e.g. "READY_FOR_REVIEW"; null leaves the current status unchanged. */
        String status) {
}
