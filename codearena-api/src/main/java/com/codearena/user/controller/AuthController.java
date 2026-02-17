package com.codearena.user.controller;

import com.codearena.common.dto.ApiResponse;
import com.codearena.user.dto.*;
import com.codearena.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User registration, login, token refresh, and logout")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<TokenResponse>> register(@Valid @RequestBody RegisterRequest request) {
        TokenResponse tokens = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(tokens));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with username/email and password")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokens = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(tokens));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse tokens = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success(tokens));
    }

    @PostMapping("/logout")
    @Operation(summary = "Invalidate tokens")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest httpRequest,
                                                       @RequestBody(required = false) LogoutRequest request) {
        String accessToken = extractToken(httpRequest);
        String refreshToken = request != null ? request.refreshToken() : null;
        authService.logout(accessToken, refreshToken);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
