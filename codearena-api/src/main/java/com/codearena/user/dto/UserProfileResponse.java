package com.codearena.user.dto;

import com.codearena.user.entity.User;

import java.time.LocalDateTime;

public record UserProfileResponse(
        Long id,
        String username,
        String email,
        String role,
        int rating,
        int maxRating,
        String avatarUrl,
        LocalDateTime createdAt
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.getRating(),
                user.getMaxRating(),
                user.getAvatarUrl(),
                user.getCreatedAt()
        );
    }
}
