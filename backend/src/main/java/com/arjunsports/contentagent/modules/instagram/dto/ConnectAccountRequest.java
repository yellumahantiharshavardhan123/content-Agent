package com.arjunsports.contentagent.modules.instagram.dto;

import jakarta.validation.constraints.NotBlank;

public record ConnectAccountRequest(
        @NotBlank(message = "businessAccountId is required")
        String businessAccountId,

        String facebookPageId,

        @NotBlank(message = "accessToken is required")
        String accessToken) {
}
