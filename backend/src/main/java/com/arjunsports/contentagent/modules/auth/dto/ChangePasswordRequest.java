package com.arjunsports.contentagent.modules.auth.dto;

import com.arjunsports.contentagent.common.validation.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Pattern(regexp = ValidationPatterns.PASSWORD_STRENGTH, message = ValidationPatterns.PASSWORD_MESSAGE)
        String newPassword) {
}
