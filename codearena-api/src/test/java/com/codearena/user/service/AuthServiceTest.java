package com.codearena.user.service;

import com.codearena.common.exception.BusinessException;
import com.codearena.common.util.JwtUtil;
import com.codearena.config.JwtConfig;
import com.codearena.user.dto.LoginRequest;
import com.codearena.user.dto.RegisterRequest;
import com.codearena.user.dto.TokenResponse;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private JwtConfig jwtConfig;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtUtil, jwtConfig, redisTemplate);
    }

    @Test
    void register_success() {
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "password123");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(jwtUtil.generateAccessToken(any(), any(), any())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any(), any(), any())).thenReturn("refresh-token");
        when(jwtConfig.getAccessExpiryMs()).thenReturn(900000L);

        TokenResponse response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateUsername_throws() {
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "password123");
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Username is already taken");
    }

    @Test
    void register_duplicateEmail_throws() {
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "password123");
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Email is already registered");
    }

    @Test
    void login_success() {
        LoginRequest request = new LoginRequest("testuser", "password123");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPasswordHash("hashed");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtUtil.generateAccessToken(any(), any(), any())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any(), any(), any())).thenReturn("refresh-token");
        when(jwtConfig.getAccessExpiryMs()).thenReturn(900000L);

        TokenResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
    }

    @Test
    void login_invalidCredentials_throws() {
        LoginRequest request = new LoginRequest("testuser", "wrongpassword");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPasswordHash("hashed");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void login_userNotFound_throws() {
        LoginRequest request = new LoginRequest("nouser", "password123");

        when(userRepository.findByUsername("nouser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("nouser")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void logout_blacklistsTokens() {
        when(jwtUtil.isValidToken("access-token")).thenReturn(true);
        when(jwtUtil.isValidToken("refresh-token")).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(jwtConfig.getAccessExpiryMs()).thenReturn(900000L);
        when(jwtConfig.getRefreshExpiryMs()).thenReturn(604800000L);

        authService.logout("access-token", "refresh-token");

        verify(valueOperations, times(2)).set(anyString(), eq("revoked"), anyLong(), any());
    }
}
