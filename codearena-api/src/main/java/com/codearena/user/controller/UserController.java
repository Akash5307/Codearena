package com.codearena.user.controller;

import com.codearena.common.dto.ApiResponse;
import com.codearena.submission.dto.SubmissionListResponse;
import com.codearena.submission.service.SubmissionService;
import com.codearena.user.dto.UpdateProfileRequest;
import com.codearena.user.dto.UserProfileResponse;
import com.codearena.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User profiles and rating leaderboard")
public class UserController {

    private final UserService userService;
    private final SubmissionService submissionService;

    public UserController(UserService userService, SubmissionService submissionService) {
        this.userService = userService;
        this.submissionService = submissionService;
    }

    @GetMapping("/{username}")
    @Operation(summary = "Get user profile by username")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(@PathVariable String username) {
        return ResponseEntity.ok(ApiResponse.success(userService.getProfile(username)));
    }

    @PutMapping("/me")
    @Operation(summary = "Update own profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(userService.updateProfile(userId, request)));
    }

    @GetMapping("/{username}/submissions")
    @Operation(summary = "Get user's submission history")
    public ResponseEntity<ApiResponse<Page<SubmissionListResponse>>> getUserSubmissions(
            @PathVariable String username, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(submissionService.getUserSubmissions(username, pageable)));
    }

    @GetMapping("/ratings")
    @Operation(summary = "Get rating leaderboard (paginated)")
    public ResponseEntity<ApiResponse<Page<UserProfileResponse>>> getRatings(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(userService.getRatingLeaderboard(pageable)));
    }
}
