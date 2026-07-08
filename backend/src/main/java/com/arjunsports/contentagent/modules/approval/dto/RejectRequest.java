package com.arjunsports.contentagent.modules.approval.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectRequest(
        @NotBlank(message = "remarks is required to reject an approval")
        String remarks) {
}
