package com.codearena.contest.dto;

import com.codearena.contest.entity.Contest;

import java.time.LocalDateTime;
import java.util.List;

public record ContestDetailResponse(
        Long id,
        String title,
        String slug,
        String description,
        String type,
        String state,
        LocalDateTime startTime,
        LocalDateTime endTime,
        int durationMinutes,
        boolean isRated,
        String authorUsername,
        long registrationCount,
        List<ContestProblemResponse> problems,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ContestDetailResponse from(Contest contest, long registrationCount,
                                              List<ContestProblemResponse> problems) {
        return new ContestDetailResponse(
                contest.getId(),
                contest.getTitle(),
                contest.getSlug(),
                contest.getDescription(),
                contest.getType().name(),
                contest.getState().name(),
                contest.getStartTime(),
                contest.getEndTime(),
                contest.getDurationMinutes(),
                contest.getIsRated(),
                contest.getAuthor().getUsername(),
                registrationCount,
                problems,
                contest.getCreatedAt(),
                contest.getUpdatedAt()
        );
    }
}
