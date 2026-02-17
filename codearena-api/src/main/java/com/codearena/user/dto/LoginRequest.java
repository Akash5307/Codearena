package com.codearena.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login request")
public record LoginRequest(
        @NotBlank(message = "Username or email is required")
        @Schema(description = "Username or email", example = "tourist")
        String usernameOrEmail,

        @NotBlank(message = "Password is required")
        @Schema(description = "Password", example = "securePass123")
        String password
) {}
