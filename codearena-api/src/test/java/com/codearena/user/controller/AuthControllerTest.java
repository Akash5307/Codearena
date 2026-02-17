package com.codearena.user.controller;

import com.codearena.common.exception.BusinessException;
import com.codearena.common.exception.GlobalExceptionHandler;
import com.codearena.common.util.JwtUtil;
import com.codearena.config.JwtAuthenticationFilter;
import com.codearena.config.SecurityConfig;
import com.codearena.user.dto.LoginRequest;
import com.codearena.user.dto.RegisterRequest;
import com.codearena.user.dto.TokenResponse;
import com.codearena.user.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthService authService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private RedisTemplate<String, Object> redisTemplate;

    @Test
    void register_returnsCreated() throws Exception {
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "password123");
        TokenResponse tokens = new TokenResponse("access", "refresh", 900);

        when(authService.register(any())).thenReturn(tokens);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh"));
    }

    @Test
    void register_invalidInput_returnsBadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest("", "", "");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_returnsOk() throws Exception {
        LoginRequest request = new LoginRequest("testuser", "password123");
        TokenResponse tokens = new TokenResponse("access", "refresh", 900);

        when(authService.login(any())).thenReturn(tokens);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access"));
    }

    @Test
    void login_invalidCredentials_returnsBadRequest() throws Exception {
        LoginRequest request = new LoginRequest("testuser", "wrong");

        when(authService.login(any())).thenThrow(new BusinessException("Invalid credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }
}
