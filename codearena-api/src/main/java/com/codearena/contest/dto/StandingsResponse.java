package com.codearena.contest.dto;

import java.util.List;

public record StandingsResponse(
        Long contestId,
        String contestTitle,
        List<StandingsEntry> entries
) {
    public record StandingsEntry(
            int rank,
            Long userId,
            String username,
            int solvedCount,
            long penaltyMinutes,
            List<ProblemResult> problemResults
    ) {}

    public record ProblemResult(
            String label,
            boolean solved,
            int attempts,
            Long solvedAtMinute
    ) {}
}
