package com.codearena.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
        String avatarUrl
) {}
