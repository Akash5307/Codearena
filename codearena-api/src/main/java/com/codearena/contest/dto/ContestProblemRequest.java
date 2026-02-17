package com.codearena.contest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ContestProblemRequest(
        @NotNull(message = "Problem ID is required")
        Long problemId,

        @NotBlank(message = "Label is required")
        String label,

        int orderIndex,

        Integer points
) {}
