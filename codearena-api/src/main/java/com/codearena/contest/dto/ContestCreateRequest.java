package com.codearena.contest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Contest creation request")
public record ContestCreateRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        @Schema(description = "Contest title", example = "CodeArena Round #1")
        String title,

        @Schema(description = "Contest description in Markdown", example = "Welcome to round 1!")
        String description,

        @NotBlank(message = "Contest type is required")
        @Schema(description = "Contest type", example = "ICPC",
                allowableValues = {"ICPC", "IOI", "EDUCATIONAL"})
        String type,

        @NotNull(message = "Start time is required")
        @Schema(description = "Contest start time (must be in the future)", example = "2026-03-01T18:00:00")
        LocalDateTime startTime,

        @NotNull(message = "Duration is required")
        @Min(value = 1, message = "Duration must be at least 1 minute")
        @Schema(description = "Contest duration in minutes", example = "120")
        Integer durationMinutes,

        @Schema(description = "Whether the contest is rated", example = "true")
        Boolean isRated,

        @Schema(description = "Problems to include in the contest")
        List<ContestProblemRequest> problems
) {}
