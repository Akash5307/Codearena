package com.codearena.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT token pair response")
public record TokenResponse(
        @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,

        @Schema(description = "JWT refresh token", example = "eyJhbGciOiJIUzI1NiJ9...")
        String refreshToken,

        @Schema(description = "Token type", example = "Bearer")
        String tokenType,

        @Schema(description = "Access token expiry in milliseconds", example = "900000")
        long expiresIn
) {
    public TokenResponse(String accessToken, String refreshToken, long expiresIn) {
        this(accessToken, refreshToken, "Bearer", expiresIn);
    }
}
