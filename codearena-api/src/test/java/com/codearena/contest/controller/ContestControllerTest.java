package com.codearena.contest.controller;

import com.codearena.common.exception.GlobalExceptionHandler;
import com.codearena.common.util.JwtUtil;
import com.codearena.config.JwtAuthenticationFilter;
import com.codearena.config.SecurityConfig;
import com.codearena.contest.dto.ContestCreateRequest;
import com.codearena.contest.dto.ContestDetailResponse;
import com.codearena.contest.dto.ContestListResponse;
import com.codearena.contest.dto.StandingsResponse;
import com.codearena.contest.service.ContestService;
import com.codearena.contest.service.StandingsService;
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

@WebMvcTest(ContestController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class ContestControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private ContestService contestService;
    @MockBean private StandingsService standingsService;
    @MockBean private com.codearena.submission.service.SubmissionService submissionService;
    @MockBean private com.codearena.contest.service.RatingService ratingService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private RedisTemplate<String, Object> redisTemplate;

    @Test
    void listContests_returnsOk() throws Exception {
        var response = new ContestListResponse(
                1L, "Round #900", "round-900", "ICPC", "BEFORE",
                LocalDateTime.now().plusDays(1), 120, true, "admin", LocalDateTime.now()
        );
        when(contestService.listContests(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/contests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].title").value("Round #900"));
    }

    @Test
    void getContest_returnsOk() throws Exception {
        var response = new ContestDetailResponse(
                1L, "Round #900", "round-900", "Fun contest", "ICPC", "BEFORE",
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2),
                120, true, "admin", 0, List.of(),
                LocalDateTime.now(), LocalDateTime.now()
        );
        when(contestService.getContestBySlug("round-900")).thenReturn(response);

        mockMvc.perform(get("/api/v1/contests/round-900"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Round #900"));
    }

    @Test
    void createContest_withoutAdmin_returns403() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken(
                1L, "user", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        ContestCreateRequest request = new ContestCreateRequest(
                "Contest", null, "ICPC", LocalDateTime.now().plusDays(1), 120, true, null
        );

        mockMvc.perform(post("/api/v1/contests")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createContest_withAdmin_returnsCreated() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken(
                1L, "admin", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        ContestCreateRequest request = new ContestCreateRequest(
                "Round #900", null, "ICPC", LocalDateTime.now().plusDays(1), 120, true, null
        );

        var response = new ContestDetailResponse(
                1L, "Round #900", "round-900", null, "ICPC", "BEFORE",
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2),
                120, true, "admin", 0, List.of(),
                LocalDateTime.now(), LocalDateTime.now()
        );
        when(contestService.createContest(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/contests")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Round #900"));
    }

    @Test
    void getStandings_returnsOk() throws Exception {
        var standings = new StandingsResponse(1L, "Round #900", List.of());
        when(standingsService.getStandings(1L)).thenReturn(standings);

        mockMvc.perform(get("/api/v1/contests/1/standings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contestTitle").value("Round #900"));
    }

    @Test
    void registerForContest_withAuth_returnsOk() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken(
                2L, "user", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        when(contestService.registerForContest(1L, 2L)).thenReturn("Successfully registered");

        mockMvc.perform(post("/api/v1/contests/1/register")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Successfully registered"));
    }

    @Test
    void registerForContest_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/contests/1/register"))
                .andExpect(status().isUnauthorized());
    }
}
