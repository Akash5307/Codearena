package com.codearena.problem.dto;

import java.util.List;

public record ProblemUpdateRequest(
        String title,
        String statement,
        String inputFormat,
        String outputFormat,
        String difficulty,
        Integer timeLimitMs,
        Integer memoryLimitMb,
        Boolean isPublished,
        List<String> tags
) {}
