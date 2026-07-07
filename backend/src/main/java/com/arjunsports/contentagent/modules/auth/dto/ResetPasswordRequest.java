package com.arjunsports.contentagent.modules.auth.dto;

import com.arjunsports.contentagent.common.validation.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(
        @NotBlank(message = "Reset token is required")
        String token,

        @NotBlank(message = "New password is required")
        @Pattern(regexp = ValidationPatterns.PASSWORD_STRENGTH, message = ValidationPatterns.PASSWORD_MESSAGE)
        String newPassword) {
}
