package com.codearena.problem.dto;

import com.codearena.problem.entity.Problem;

import java.time.LocalDateTime;
import java.util.List;

public record ProblemListResponse(
        Long id,
        String title,
        String slug,
        String difficulty,
        int timeLimitMs,
        int memoryLimitMb,
        String authorUsername,
        List<String> tags,
        LocalDateTime createdAt
) {
    public static ProblemListResponse from(Problem problem) {
        return new ProblemListResponse(
                problem.getId(),
                problem.getTitle(),
                problem.getSlug(),
                problem.getDifficulty() != null ? problem.getDifficulty().name() : null,
                problem.getTimeLimitMs(),
                problem.getMemoryLimitMb(),
                problem.getAuthor().getUsername(),
                problem.getTags().stream().map(t -> t.getName()).toList(),
                problem.getCreatedAt()
        );
    }
}
