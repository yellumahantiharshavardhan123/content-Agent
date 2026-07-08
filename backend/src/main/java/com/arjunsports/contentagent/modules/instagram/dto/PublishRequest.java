package com.arjunsports.contentagent.modules.instagram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PublishRequest(
        @NotNull(message = "approvalId is required")
        UUID approvalId,

        @NotNull(message = "mediaId is required")
        UUID mediaId,

        @NotBlank(message = "caption is required")
        String caption,

        String hashtags) {
}
