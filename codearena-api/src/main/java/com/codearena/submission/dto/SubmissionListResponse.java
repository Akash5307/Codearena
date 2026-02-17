package com.codearena.submission.dto;

import com.codearena.submission.entity.Submission;

import java.time.LocalDateTime;

public record SubmissionListResponse(
        Long id,
        String username,
        Long problemId,
        String problemTitle,
        Long contestId,
        String language,
        String verdict,
        Integer timeUsedMs,
        Integer memoryUsedKb,
        LocalDateTime submittedAt
) {
    public static SubmissionListResponse from(Submission s) {
        return new SubmissionListResponse(
                s.getId(),
                s.getUser().getUsername(),
                s.getProblem().getId(),
                s.getProblem().getTitle(),
                s.getContest() != null ? s.getContest().getId() : null,
                s.getLanguage().name(),
                s.getVerdict().name(),
                s.getTimeUsedMs(),
                s.getMemoryUsedKb(),
                s.getSubmittedAt()
        );
    }
}
