package com.codearena.user.service;

import com.codearena.common.exception.BusinessException;
import com.codearena.common.util.JwtUtil;
import com.codearena.config.JwtConfig;
import com.codearena.user.dto.*;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtConfig jwtConfig;
    private final RedisTemplate<String, Object> redisTemplate;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       JwtConfig jwtConfig,
                       RedisTemplate<String, Object> redisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.jwtConfig = jwtConfig;
        this.redisTemplate = redisTemplate;
    }

    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email is already registered");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user = userRepository.save(user);

        return generateTokenPair(user);
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.usernameOrEmail())
                .or(() -> userRepository.findByEmail(request.usernameOrEmail()))
                .orElseThrow(() -> new BusinessException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("Invalid credentials");
        }

        return generateTokenPair(user);
    }

    public TokenResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        if (!jwtUtil.isValidToken(refreshToken)) {
            throw new BusinessException("Invalid refresh token");
        }
        if (!"refresh".equals(jwtUtil.getTokenType(refreshToken))) {
            throw new BusinessException("Invalid token type");
        }

        Boolean isBlacklisted = redisTemplate.hasKey("blacklist:" + refreshToken);
        if (Boolean.TRUE.equals(isBlacklisted)) {
            throw new BusinessException("Refresh token has been revoked");
        }

        Long userId = jwtUtil.getUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        // Blacklist the old refresh token
        redisTemplate.opsForValue().set("blacklist:" + refreshToken, "revoked",
                jwtConfig.getRefreshExpiryMs(), TimeUnit.MILLISECONDS);

        return generateTokenPair(user);
    }

    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null && jwtUtil.isValidToken(accessToken)) {
            redisTemplate.opsForValue().set("blacklist:" + accessToken, "revoked",
                    jwtConfig.getAccessExpiryMs(), TimeUnit.MILLISECONDS);
        }
        if (refreshToken != null && jwtUtil.isValidToken(refreshToken)) {
            redisTemplate.opsForValue().set("blacklist:" + refreshToken, "revoked",
                    jwtConfig.getRefreshExpiryMs(), TimeUnit.MILLISECONDS);
        }
    }

    private TokenResponse generateTokenPair(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getRole().name());
        return new TokenResponse(accessToken, refreshToken, jwtConfig.getAccessExpiryMs() / 1000);
    }
}
