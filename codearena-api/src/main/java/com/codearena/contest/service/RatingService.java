package com.codearena.contest.service;

import com.codearena.common.exception.BusinessException;
import com.codearena.common.exception.ResourceNotFoundException;
import com.codearena.contest.dto.StandingsResponse;
import com.codearena.contest.entity.Contest;
import com.codearena.contest.entity.RatingChange;
import com.codearena.contest.repository.ContestRepository;
import com.codearena.contest.repository.RatingChangeRepository;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Recomputes user ratings after a rated contest ends, using the Codeforces rating
 * algorithm (Mike Mirzayanov): each contestant's expected "seed" rank is derived
 * from pairwise Elo win probabilities, combined with their actual rank, converted
 * back to a performance rating, and halved into a delta — then two fairness passes
 * keep the total change near zero and protect top performers.
 *
 * Idempotent: a contest is processed at most once (guarded by `ratingsApplied`).
 */
@Service
public class RatingService {

    private static final Logger log = LoggerFactory.getLogger(RatingService.class);

    private final ContestRepository contestRepository;
    private final UserRepository userRepository;
    private final RatingChangeRepository ratingChangeRepository;
    private final StandingsService standingsService;
    private final CacheManager cacheManager;

    public RatingService(ContestRepository contestRepository,
                         UserRepository userRepository,
                         RatingChangeRepository ratingChangeRepository,
                         StandingsService standingsService,
                         CacheManager cacheManager) {
        this.contestRepository = contestRepository;
        this.userRepository = userRepository;
        this.ratingChangeRepository = ratingChangeRepository;
        this.standingsService = standingsService;
        this.cacheManager = cacheManager;
    }

    /** A contestant during the calculation; rating is the pre-contest rating. */
    private static final class Seed {
        final long userId;
        final String username;
        final int rating;
        int rank;          // tie-aware 1-based place
        int delta;
        Seed(long userId, String username, int rating) {
            this.userId = userId;
            this.username = username;
            this.rating = rating;
        }
    }

    /**
     * Apply ratings for a contest. Returns the number of participants rated.
     * Throws BusinessException if the contest is not eligible (not rated, not ended,
     * or already processed) so the manual admin trigger surfaces a clear message.
     */
    @Transactional
    public int applyRatings(Long contestId) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new ResourceNotFoundException("Contest", "id", contestId));

        if (!contest.getIsRated()) {
            throw new BusinessException("Contest is not rated");
        }
        if (contest.getState() != Contest.ContestState.ENDED) {
            throw new BusinessException("Contest has not ended yet");
        }
        if (Boolean.TRUE.equals(contest.getRatingsApplied())) {
            throw new BusinessException("Ratings have already been applied for this contest");
        }

        StandingsResponse standings = standingsService.getStandings(contestId);
        List<StandingsResponse.StandingsEntry> entries = standings.entries();

        // Build the field with each user's current (pre-contest) rating and a
        // tie-aware place: contestants tied on (solved, penalty) share a rank.
        List<Seed> seeds = new ArrayList<>();
        int place = 0;
        Integer prevSolved = null;
        Long prevPenalty = null;
        for (int i = 0; i < entries.size(); i++) {
            StandingsResponse.StandingsEntry e = entries.get(i);
            User user = userRepository.findById(e.userId()).orElse(null);
            if (user == null) continue;
            boolean tie = prevSolved != null
                    && prevSolved == e.solvedCount() && prevPenalty == e.penaltyMinutes();
            if (!tie) place = i + 1;
            prevSolved = e.solvedCount();
            prevPenalty = e.penaltyMinutes();

            Seed s = new Seed(user.getId(), user.getUsername(), user.getRating());
            s.rank = place;
            seeds.add(s);
        }

        if (seeds.size() < 2) {
            // A rating change needs at least two competitors to be meaningful.
            contest.setRatingsApplied(true);
            contestRepository.save(contest);
            return 0;
        }

        computeDeltas(seeds);

        Cache userCache = cacheManager.getCache("userProfile");
        int rated = 0;
        for (Seed s : seeds) {
            User user = userRepository.findById(s.userId).orElse(null);
            if (user == null) continue;
            int oldRating = user.getRating();
            int newRating = oldRating + s.delta;
            user.setRating(newRating);
            if (newRating > user.getMaxRating()) {
                user.setMaxRating(newRating);
            }
            userRepository.save(user);
            ratingChangeRepository.save(
                    new RatingChange(contestId, s.userId, oldRating, newRating, s.delta));
            if (userCache != null) userCache.evict(s.username);
            rated++;
        }

        contest.setRatingsApplied(true);
        contestRepository.save(contest);
        log.info("Applied ratings for contest {} to {} participants", contestId, rated);
        return rated;
    }

    // --- Codeforces rating math ---

    /** Expected seed (rank) of `rating` against everyone except contestant `skip`. */
    private double seed(double rating, List<Seed> all, int skip) {
        double result = 1.0;
        for (int j = 0; j < all.size(); j++) {
            if (j == skip) continue;
            result += 1.0 / (1.0 + Math.pow(10.0, (rating - all.get(j).rating) / 400.0));
        }
        return result;
    }

    /** Rating whose expected seed equals `targetSeed` (seed is decreasing in rating). */
    private int ratingForSeed(double targetSeed, List<Seed> all, int skip) {
        int lo = 1, hi = 8000;
        while (lo < hi) {
            int mid = (lo + hi) / 2;
            if (seed(mid, all, skip) < targetSeed) {
                hi = mid;
            } else {
                lo = mid + 1;
            }
        }
        return lo;
    }

    private void computeDeltas(List<Seed> seeds) {
        int n = seeds.size();
        for (int i = 0; i < n; i++) {
            Seed s = seeds.get(i);
            double midRank = Math.sqrt((double) s.rank * seed(s.rating, seeds, i));
            int performance = ratingForSeed(midRank, seeds, i);
            s.delta = (performance - s.rating) / 2;
        }

        // Pass 1: shift so the total change is slightly negative (anti-inflation).
        seeds.sort((a, b) -> Integer.compare(b.rating, a.rating));
        long sum = seeds.stream().mapToLong(s -> s.delta).sum();
        int inc1 = (int) Math.round(-sum / (double) n) - 1;
        for (Seed s : seeds) s.delta += inc1;

        // Pass 2: keep the sum over the strongest cohort from drifting (protect the top).
        int cohort = Math.min((int) Math.round(4 * Math.sqrt(n)), n);
        long topSum = 0;
        for (int i = 0; i < cohort; i++) topSum += seeds.get(i).delta;
        int inc2 = (int) Math.min(Math.max(Math.round(-topSum / (double) cohort), -10), 0);
        for (Seed s : seeds) s.delta += inc2;
    }
}
