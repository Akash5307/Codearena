package com.codearena.blog.controller;

import com.codearena.blog.dto.*;
import com.codearena.blog.service.BlogService;
import com.codearena.common.dto.ApiResponse;
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
@RequestMapping("/api/v1/blogs")
@Tag(name = "Blogs", description = "Blog posts, comments, and voting")
public class BlogController {

    private final BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    @GetMapping
    @Operation(summary = "List blog posts (paginated)")
    public ResponseEntity<ApiResponse<Page<BlogPostListResponse>>> listPosts(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(blogService.listPosts(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get blog post detail with threaded comments")
    public ResponseEntity<ApiResponse<BlogPostDetailResponse>> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(blogService.getPost(id)));
    }

    @PostMapping
    @Operation(summary = "Create a blog post")
    public ResponseEntity<ApiResponse<BlogPostDetailResponse>> createPost(
            Authentication authentication,
            @Valid @RequestBody BlogPostCreateRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(blogService.createPost(userId, request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit a blog post (own posts only)")
    public ResponseEntity<ApiResponse<BlogPostDetailResponse>> updatePost(
            @PathVariable Long id,
            Authentication authentication,
            @RequestBody BlogPostUpdateRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(blogService.updatePost(id, userId, request)));
    }

    @PostMapping("/{id}/vote")
    @Operation(summary = "Upvote or downvote a blog post (toggleable)")
    public ResponseEntity<ApiResponse<String>> vote(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody VoteRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(blogService.vote(id, userId, request)));
    }

    @PostMapping("/{id}/comments")
    @Operation(summary = "Add a comment (supports threaded replies via parentId)")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody CommentCreateRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(blogService.addComment(id, userId, request)));
    }
}
