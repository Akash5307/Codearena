package com.codearena.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Blog post creation request")
public record BlogPostCreateRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        @Schema(description = "Post title", example = "Tutorial: Segment Trees")
        String title,

        @NotBlank(message = "Content is required")
        @Schema(description = "Post content in Markdown", example = "# Segment Trees\n\nA segment tree is...")
        String content
) {}
