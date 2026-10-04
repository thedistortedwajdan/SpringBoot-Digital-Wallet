package com.app.wallet.dto;

import com.app.wallet.model.User;

import java.time.LocalDateTime;

public record UserResponseDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        String role,
        boolean active,
        LocalDateTime createdAt) {

    public static UserResponseDto from(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt());
    }
}
