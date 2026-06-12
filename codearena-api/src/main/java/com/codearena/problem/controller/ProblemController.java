package com.codearena.problem.controller;

import com.codearena.common.dto.ApiResponse;
import com.codearena.problem.dto.*;
import com.codearena.problem.service.ProblemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/problems")
@Tag(name = "Problems", description = "Problem management and browsing")
public class ProblemController {

    private final ProblemService problemService;

    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    @GetMapping
    @Operation(summary = "List problems with optional filters")
    public ResponseEntity<ApiResponse<Page<ProblemListResponse>>> listProblems(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String tag,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                problemService.listProblems(title, difficulty, tag, pageable)));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get problem detail by slug")
    public ResponseEntity<ApiResponse<ProblemDetailResponse>> getProblem(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(problemService.getProblemBySlug(slug)));
    }

    @GetMapping("/{slug}/samples")
    @Operation(summary = "Get the sample test cases (input/output text) for a problem")
    public ResponseEntity<ApiResponse<List<SampleTestCaseResponse>>> getSampleTestCases(
            @PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(problemService.getSampleTestCases(slug)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PROBLEM_SETTER', 'ADMIN')")
    @Operation(summary = "Create a new problem")
    public ResponseEntity<ApiResponse<ProblemDetailResponse>> createProblem(
            Authentication authentication,
            @Valid @RequestBody ProblemCreateRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(problemService.createProblem(userId, request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROBLEM_SETTER', 'ADMIN')")
    @Operation(summary = "Update a problem")
    public ResponseEntity<ApiResponse<ProblemDetailResponse>> updateProblem(
            @PathVariable Long id,
            @RequestBody ProblemUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(problemService.updateProblem(id, request)));
    }

    @PostMapping(value = "/{id}/test-cases", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('PROBLEM_SETTER', 'ADMIN')")
    @Operation(summary = "Upload a test case for a problem")
    public ResponseEntity<ApiResponse<TestCaseResponse>> addTestCase(
            @PathVariable Long id,
            @RequestParam("input") MultipartFile inputFile,
            @RequestParam("output") MultipartFile outputFile,
            @RequestParam(value = "sample", defaultValue = "false") boolean isSample) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(problemService.addTestCase(id, inputFile, outputFile, isSample)));
    }

    @GetMapping("/tags")
    @Operation(summary = "List all tags")
    public ResponseEntity<ApiResponse<List<TagResponse>>> getAllTags() {
        return ResponseEntity.ok(ApiResponse.success(problemService.getAllTags()));
    }
}
