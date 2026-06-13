package com.codearena.contest.service;

import com.codearena.contest.entity.Contest;
import com.codearena.contest.repository.ContestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls for rated contests that have ended but not yet been rated and applies
 * ratings automatically. Since contest state is computed from the clock (not
 * stored), there's no ENDED event to hook — a short poll is the simplest reliable
 * trigger. The per-contest `ratings_applied` flag makes this idempotent.
 */
@Component
public class RatingScheduler {

    private static final Logger log = LoggerFactory.getLogger(RatingScheduler.class);

    private final ContestRepository contestRepository;
    private final RatingService ratingService;

    public RatingScheduler(ContestRepository contestRepository, RatingService ratingService) {
        this.contestRepository = contestRepository;
        this.ratingService = ratingService;
    }

    @Scheduled(fixedDelayString = "${rating.poll-interval-ms:60000}")
    public void processEndedContests() {
        for (Contest contest : contestRepository.findByIsRatedTrueAndRatingsAppliedFalse()) {
            if (contest.getState() != Contest.ContestState.ENDED) continue;
            try {
                ratingService.applyRatings(contest.getId());
            } catch (Exception e) {
                log.error("Failed to apply ratings for contest {}", contest.getId(), e);
            }
        }
    }
}
