package com.arjunsports.contentagent.modules.approval.dto;

import jakarta.validation.constraints.NotBlank;

public record AddCommentRequest(
        @NotBlank(message = "comment is required")
        String comment) {
}
