package com.codearena.contest.dto;

import com.codearena.contest.entity.Contest;

import java.time.LocalDateTime;

public record ContestListResponse(
        Long id,
        String title,
        String slug,
        String type,
        String state,
        LocalDateTime startTime,
        int durationMinutes,
        boolean isRated,
        String authorUsername,
        LocalDateTime createdAt
) {
    public static ContestListResponse from(Contest contest) {
        return new ContestListResponse(
                contest.getId(),
                contest.getTitle(),
                contest.getSlug(),
                contest.getType().name(),
                contest.getState().name(),
                contest.getStartTime(),
                contest.getDurationMinutes(),
                contest.getIsRated(),
                contest.getAuthor().getUsername(),
                contest.getCreatedAt()
        );
    }
}
