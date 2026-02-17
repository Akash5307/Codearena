package com.codearena.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Comment creation request")
public record CommentCreateRequest(
        @NotBlank(message = "Content is required")
        @Schema(description = "Comment text", example = "Great explanation, thanks!")
        String content,

        @Schema(description = "Parent comment ID for threaded replies (null for top-level)", example = "1")
        Long parentId
) {}
