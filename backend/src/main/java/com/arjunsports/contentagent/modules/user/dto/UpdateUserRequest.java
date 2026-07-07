package com.arjunsports.contentagent.modules.user.dto;

import com.arjunsports.contentagent.common.validation.ValidationPatterns;
import com.arjunsports.contentagent.modules.user.Role;
import com.arjunsports.contentagent.modules.user.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100)
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100)
        String lastName,

        @Pattern(regexp = ValidationPatterns.PHONE, message = ValidationPatterns.PHONE_MESSAGE)
        String phone,

        @NotNull(message = "Role is required")
        Role role,

        @NotNull(message = "Status is required")
        UserStatus status,

        boolean active) {
}
