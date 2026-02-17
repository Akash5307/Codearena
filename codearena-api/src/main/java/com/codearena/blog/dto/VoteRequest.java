package com.codearena.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Blog post vote request")
public record VoteRequest(
        @NotBlank(message = "Vote type is required")
        @Pattern(regexp = "UPVOTE|DOWNVOTE", message = "Vote type must be UPVOTE or DOWNVOTE")
        @Schema(description = "Vote type", example = "UPVOTE", allowableValues = {"UPVOTE", "DOWNVOTE"})
        String voteType
) {}
