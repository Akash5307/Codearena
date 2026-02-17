package com.codearena.submission.dto;

import com.codearena.submission.entity.Submission;

import java.time.LocalDateTime;

public record SubmissionDetailResponse(
        Long id,
        String username,
        Long problemId,
        String problemTitle,
        Long contestId,
        String language,
        String sourceCode,
        String verdict,
        Integer timeUsedMs,
        Integer memoryUsedKb,
        int testCasesPassed,
        int totalTestCases,
        LocalDateTime submittedAt,
        LocalDateTime judgedAt
) {
    public static SubmissionDetailResponse from(Submission s) {
        return new SubmissionDetailResponse(
                s.getId(),
                s.getUser().getUsername(),
                s.getProblem().getId(),
                s.getProblem().getTitle(),
                s.getContest() != null ? s.getContest().getId() : null,
                s.getLanguage().name(),
                s.getSourceCode(),
                s.getVerdict().name(),
                s.getTimeUsedMs(),
                s.getMemoryUsedKb(),
                s.getTestCasesPassed(),
                s.getTotalTestCases(),
                s.getSubmittedAt(),
                s.getJudgedAt()
        );
    }
}
