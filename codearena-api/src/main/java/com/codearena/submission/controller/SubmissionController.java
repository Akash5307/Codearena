package com.codearena.submission.controller;

import com.codearena.common.dto.ApiResponse;
import com.codearena.submission.dto.SubmissionDetailResponse;
import com.codearena.submission.dto.SubmissionListResponse;
import com.codearena.submission.dto.SubmitRequest;
import com.codearena.submission.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/submissions")
@Tag(name = "Submissions", description = "Code submission and verdict tracking")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping
    @Operation(summary = "Submit a solution")
    public ResponseEntity<ApiResponse<SubmissionDetailResponse>> submit(
            Authentication authentication,
            @Valid @RequestBody SubmitRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(submissionService.submit(userId, request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get submission detail with verdict")
    public ResponseEntity<ApiResponse<SubmissionDetailResponse>> getSubmission(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(submissionService.getSubmission(id)));
    }

    @GetMapping
    @Operation(summary = "List recent submissions (filterable)")
    public ResponseEntity<ApiResponse<Page<SubmissionListResponse>>> listSubmissions(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long problemId,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String verdict,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                submissionService.listSubmissions(userId, problemId, language, verdict, pageable)));
    }
}
