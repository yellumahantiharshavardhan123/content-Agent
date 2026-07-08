package com.arjunsports.contentagent.modules.approval.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SubmitApprovalRequest(
        @NotNull(message = "contentId is required")
        UUID contentId) {
}
