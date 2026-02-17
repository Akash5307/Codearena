package com.codearena.problem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Problem creation request")
public record ProblemCreateRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        @Schema(description = "Problem title", example = "Two Sum")
        String title,

        @NotBlank(message = "Problem statement is required")
        @Schema(description = "Problem statement in Markdown", example = "Given an array of integers...")
        String statement,

        @Schema(description = "Input format description", example = "First line contains n")
        String inputFormat,

        @Schema(description = "Output format description", example = "Print the answer")
        String outputFormat,

        @Schema(description = "Difficulty level", example = "MEDIUM",
                allowableValues = {"EASY", "MEDIUM", "HARD"})
        String difficulty,

        @Schema(description = "Time limit in milliseconds", example = "2000")
        Integer timeLimitMs,

        @Schema(description = "Memory limit in megabytes", example = "256")
        Integer memoryLimitMb,

        @Schema(description = "Problem tags", example = "[\"dp\", \"greedy\"]")
        List<String> tags
) {}
