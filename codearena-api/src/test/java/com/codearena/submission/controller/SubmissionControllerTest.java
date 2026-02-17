package com.codearena.submission.controller;

import com.codearena.common.exception.GlobalExceptionHandler;
import com.codearena.common.util.JwtUtil;
import com.codearena.config.JwtAuthenticationFilter;
import com.codearena.config.SecurityConfig;
import com.codearena.submission.dto.SubmissionDetailResponse;
import com.codearena.submission.dto.SubmissionListResponse;
import com.codearena.submission.dto.SubmitRequest;
import com.codearena.submission.service.SubmissionService;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SubmissionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class SubmissionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private SubmissionService submissionService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private RedisTemplate<String, Object> redisTemplate;

    private UsernamePasswordAuthenticationToken userAuth() {
        return new UsernamePasswordAuthenticationToken(
                1L, "testuser", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void submit_withAuth_returnsCreated() throws Exception {
        SubmitRequest request = new SubmitRequest(1L, null, "CPP", "int main() {}");

        var response = new SubmissionDetailResponse(
                1L, "testuser", 1L, "Two Sum", null,
                "CPP", "int main() {}", "PENDING",
                null, null, 0, 0, LocalDateTime.now(), null
        );
        when(submissionService.submit(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/submissions")
                        .with(authentication(userAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.verdict").value("PENDING"));
    }

    @Test
    void submit_withoutAuth_returns403() throws Exception {
        SubmitRequest request = new SubmitRequest(1L, null, "CPP", "int main() {}");

        mockMvc.perform(post("/api/v1/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listSubmissions_returnsOk() throws Exception {
        var response = new SubmissionListResponse(
                1L, "testuser", 1L, "Two Sum", null,
                "CPP", "AC", 50, 1024, LocalDateTime.now()
        );
        when(submissionService.listSubmissions(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/submissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].verdict").value("AC"));
    }

    @Test
    void getSubmission_returnsOk() throws Exception {
        var response = new SubmissionDetailResponse(
                1L, "testuser", 1L, "Two Sum", null,
                "CPP", "int main() {}", "AC",
                50, 1024, 5, 5, LocalDateTime.now(), LocalDateTime.now()
        );
        when(submissionService.getSubmission(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/submissions/1")
                        .with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verdict").value("AC"))
                .andExpect(jsonPath("$.data.testCasesPassed").value(5));
    }
}
