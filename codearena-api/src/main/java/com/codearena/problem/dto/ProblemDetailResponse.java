package com.codearena.problem.dto;

import com.codearena.problem.entity.Problem;

import java.time.LocalDateTime;
import java.util.List;

public record ProblemDetailResponse(
        Long id,
        String title,
        String slug,
        String statement,
        String inputFormat,
        String outputFormat,
        String difficulty,
        int timeLimitMs,
        int memoryLimitMb,
        String authorUsername,
        boolean isPublished,
        List<String> tags,
        List<TestCaseResponse> sampleTestCases,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProblemDetailResponse from(Problem problem, List<TestCaseResponse> sampleTestCases) {
        return new ProblemDetailResponse(
                problem.getId(),
                problem.getTitle(),
                problem.getSlug(),
                problem.getStatement(),
                problem.getInputFormat(),
                problem.getOutputFormat(),
                problem.getDifficulty() != null ? problem.getDifficulty().name() : null,
                problem.getTimeLimitMs(),
                problem.getMemoryLimitMb(),
                problem.getAuthor().getUsername(),
                problem.getIsPublished(),
                problem.getTags().stream().map(t -> t.getName()).toList(),
                sampleTestCases,
                problem.getCreatedAt(),
                problem.getUpdatedAt()
        );
    }
}
