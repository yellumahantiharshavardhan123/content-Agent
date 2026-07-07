package com.arjunsports.contentagent.modules.user.dto;

import com.arjunsports.contentagent.modules.user.Role;
import com.arjunsports.contentagent.modules.user.User;
import com.arjunsports.contentagent.modules.user.UserStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        Role role,
        UserStatus status,
        boolean active,
        Instant lastLogin,
        Instant createdAt,
        Instant updatedAt) {

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .active(user.isActive())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
