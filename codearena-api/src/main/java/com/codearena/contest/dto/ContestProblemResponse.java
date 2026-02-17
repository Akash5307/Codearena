package com.codearena.contest.dto;

import com.codearena.contest.entity.ContestProblem;

public record ContestProblemResponse(
        Long problemId,
        String problemTitle,
        String label,
        int orderIndex,
        Integer points
) {
    public static ContestProblemResponse from(ContestProblem cp) {
        return new ContestProblemResponse(
                cp.getProblem().getId(),
                cp.getProblem().getTitle(),
                cp.getLabel(),
                cp.getOrderIndex(),
                cp.getPoints()
        );
    }
}
