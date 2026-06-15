package com.codearena.problem.controller;

import com.codearena.common.exception.GlobalExceptionHandler;
import com.codearena.common.util.JwtUtil;
import com.codearena.config.JwtAuthenticationFilter;
import com.codearena.config.SecurityConfig;
import com.codearena.problem.dto.*;
import com.codearena.problem.service.ProblemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProblemController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class ProblemControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private ProblemService problemService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private RedisTemplate<String, Object> redisTemplate;

    @Test
    void listProblems_returnsOk() throws Exception {
        var response = new ProblemListResponse(
                1L, "Two Sum", "two-sum", "EASY", 2000, 256,
                "setter", List.of("dp"), LocalDateTime.now()
        );
        when(problemService.listProblems(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/problems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].title").value("Two Sum"));
    }

    @Test
    void getProblem_returnsOk() throws Exception {
        var response = new ProblemDetailResponse(
                1L, "Two Sum", "two-sum", "Find two numbers...",
                "Input", "Output", "EASY", 2000, 256,
                "setter", true, List.of("dp"), List.of(),
                LocalDateTime.now(), LocalDateTime.now()
        );
        when(problemService.getProblemBySlug("two-sum")).thenReturn(response);

        mockMvc.perform(get("/api/v1/problems/two-sum"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Two Sum"));
    }

    @Test
    void createProblem_withoutAuth_returns401() throws Exception {
        ProblemCreateRequest request = new ProblemCreateRequest(
                "Title", "Statement", null, null, null, null, null, null
        );

        mockMvc.perform(post("/api/v1/problems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProblem_withRole_returnsCreated() throws Exception {
        ProblemCreateRequest request = new ProblemCreateRequest(
                "Title", "Statement", null, null, "EASY", null, null, null
        );
        var response = new ProblemDetailResponse(
                1L, "Title", "title", "Statement",
                null, null, "EASY", 2000, 256,
                "setter", false, List.of(), List.of(),
                LocalDateTime.now(), LocalDateTime.now()
        );
        when(problemService.createProblem(any(), any())).thenReturn(response);

        var auth = new UsernamePasswordAuthenticationToken(
                1L, "setter", List.of(new SimpleGrantedAuthority("ROLE_PROBLEM_SETTER")));

        mockMvc.perform(post("/api/v1/problems")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Title"));
    }

    @Test
    void getTags_returnsOk() throws Exception {
        when(problemService.getAllTags()).thenReturn(
                List.of(new TagResponse(1L, "dp"), new TagResponse(2L, "greedy")));

        mockMvc.perform(get("/api/v1/problems/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("dp"));
    }
}
