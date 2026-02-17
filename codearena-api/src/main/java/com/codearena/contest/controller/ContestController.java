package com.codearena.contest.controller;

import com.codearena.common.dto.ApiResponse;
import com.codearena.contest.dto.ContestCreateRequest;
import com.codearena.contest.dto.ContestDetailResponse;
import com.codearena.contest.dto.ContestListResponse;
import com.codearena.contest.dto.StandingsResponse;
import com.codearena.contest.service.ContestService;
import com.codearena.contest.service.StandingsService;
import com.codearena.submission.dto.SubmissionListResponse;
import com.codearena.submission.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/contests")
@Tag(name = "Contests", description = "Contest management, registration, and standings")
public class ContestController {

    private final ContestService contestService;
    private final StandingsService standingsService;
    private final SubmissionService submissionService;

    public ContestController(ContestService contestService, StandingsService standingsService,
                             SubmissionService submissionService) {
        this.contestService = contestService;
        this.standingsService = standingsService;
        this.submissionService = submissionService;
    }

    @GetMapping
    @Operation(summary = "List contests (filter by status: upcoming, running, past)")
    public ResponseEntity<ApiResponse<Page<ContestListResponse>>> listContests(
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(contestService.listContests(status, pageable)));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get contest detail with problems")
    public ResponseEntity<ApiResponse<ContestDetailResponse>> getContest(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(contestService.getContestBySlug(slug)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new contest (ADMIN only)")
    public ResponseEntity<ApiResponse<ContestDetailResponse>> createContest(
            Authentication authentication,
            @Valid @RequestBody ContestCreateRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(contestService.createContest(userId, request)));
    }

    @PostMapping("/{id}/register")
    @Operation(summary = "Register for a contest")
    public ResponseEntity<ApiResponse<String>> registerForContest(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(contestService.registerForContest(id, userId)));
    }

    @GetMapping("/{id}/standings")
    @Operation(summary = "Get contest standings")
    public ResponseEntity<ApiResponse<StandingsResponse>> getStandings(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(standingsService.getStandings(id)));
    }

    @GetMapping("/{id}/my-submissions")
    @Operation(summary = "Get my submissions in a contest")
    public ResponseEntity<ApiResponse<Page<SubmissionListResponse>>> getMyContestSubmissions(
            @PathVariable Long id,
            Authentication authentication,
            Pageable pageable) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(
                submissionService.getMyContestSubmissions(id, userId, pageable)));
    }
}
