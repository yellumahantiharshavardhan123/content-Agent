package com.arjunsports.contentagent.modules.user.dto;

import com.arjunsports.contentagent.common.validation.ValidationPatterns;
import com.arjunsports.contentagent.modules.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100)
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100)
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        String email,

        @Pattern(regexp = ValidationPatterns.PHONE, message = ValidationPatterns.PHONE_MESSAGE)
        String phone,

        @NotBlank(message = "Password is required")
        @Pattern(regexp = ValidationPatterns.PASSWORD_STRENGTH, message = ValidationPatterns.PASSWORD_MESSAGE)
        String password,

        @NotNull(message = "Role is required")
        Role role) {
}
