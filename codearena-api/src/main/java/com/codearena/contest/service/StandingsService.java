package com.codearena.contest.service;

import com.codearena.common.exception.ResourceNotFoundException;
import com.codearena.contest.dto.StandingsResponse;
import com.codearena.contest.entity.Contest;
import com.codearena.contest.entity.ContestProblem;
import com.codearena.contest.repository.ContestProblemRepository;
import com.codearena.contest.repository.ContestRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StandingsService {

    private static final String STANDINGS_CACHE_KEY = "standings:contest:";

    private final ContestRepository contestRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final JdbcTemplate jdbcTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    public StandingsService(ContestRepository contestRepository,
                            ContestProblemRepository contestProblemRepository,
                            JdbcTemplate jdbcTemplate,
                            RedisTemplate<String, Object> redisTemplate) {
        this.contestRepository = contestRepository;
        this.contestProblemRepository = contestProblemRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    @SuppressWarnings("unchecked")
    public StandingsResponse getStandings(Long contestId) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new ResourceNotFoundException("Contest", "id", contestId));

        // Try cache first
        String cacheKey = STANDINGS_CACHE_KEY + contestId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof StandingsResponse sr) {
            return sr;
        }

        StandingsResponse standings = computeStandings(contest);

        // Cache for 30 seconds during running contests, 5 minutes for ended
        Duration ttl = contest.getState() == Contest.ContestState.RUNNING
                ? Duration.ofSeconds(30)
                : Duration.ofMinutes(5);
        redisTemplate.opsForValue().set(cacheKey, standings, ttl);

        return standings;
    }

    public void invalidateStandingsCache(Long contestId) {
        redisTemplate.delete(STANDINGS_CACHE_KEY + contestId);
    }

    private StandingsResponse computeStandings(Contest contest) {
        List<ContestProblem> contestProblems = contestProblemRepository
                .findByContestIdOrderByOrderIndex(contest.getId());

        if (contestProblems.isEmpty()) {
            return new StandingsResponse(contest.getId(), contest.getTitle(), List.of());
        }

        // Build label -> problemId map
        Map<Long, String> problemIdToLabel = contestProblems.stream()
                .collect(Collectors.toMap(cp -> cp.getProblem().getId(), ContestProblem::getLabel));

        List<Long> problemIds = contestProblems.stream()
                .map(cp -> cp.getProblem().getId())
                .toList();

        // Query submissions for this contest
        // ICPC style: first AC counts, penalty = time to AC + 20 * wrong attempts
        String sql = """
                SELECT s.user_id, u.username, s.problem_id, s.verdict, s.submitted_at
                FROM submissions s
                JOIN users u ON u.id = s.user_id
                WHERE s.contest_id = ?
                AND s.problem_id IN (%s)
                ORDER BY s.submitted_at ASC
                """.formatted(problemIds.stream().map(String::valueOf).collect(Collectors.joining(",")));

        Map<Long, UserStandingsData> userDataMap = new LinkedHashMap<>();

        try {
            jdbcTemplate.query(sql, new Object[]{contest.getId()}, rs -> {
                long userId = rs.getLong("user_id");
                String username = rs.getString("username");
                long problemId = rs.getLong("problem_id");
                String verdict = rs.getString("verdict");
                LocalDateTime submittedAt = rs.getTimestamp("submitted_at").toLocalDateTime();

                UserStandingsData data = userDataMap.computeIfAbsent(userId,
                        id -> new UserStandingsData(userId, username));

                String label = problemIdToLabel.get(problemId);
                if (label == null) return;

                ProblemData pd = data.problems.computeIfAbsent(label, l -> new ProblemData());
                if (pd.solved) return; // already solved

                if ("AC".equals(verdict)) {
                    pd.solved = true;
                    pd.solvedAt = submittedAt;
                    long minutesToSolve = Duration.between(contest.getStartTime(), submittedAt).toMinutes();
                    pd.solvedAtMinute = minutesToSolve;
                    data.solvedCount++;
                    data.penaltyMinutes += minutesToSolve + (pd.attempts * 20L);
                } else {
                    pd.attempts++;
                }
            });
        } catch (Exception e) {
            // submissions table may not exist yet; return empty standings
            return new StandingsResponse(contest.getId(), contest.getTitle(), List.of());
        }

        // Sort: most solved first, then least penalty
        List<UserStandingsData> sorted = userDataMap.values().stream()
                .sorted(Comparator.comparingInt((UserStandingsData d) -> d.solvedCount).reversed()
                        .thenComparingLong(d -> d.penaltyMinutes))
                .toList();

        List<String> labels = contestProblems.stream().map(ContestProblem::getLabel).toList();

        List<StandingsResponse.StandingsEntry> entries = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            UserStandingsData d = sorted.get(i);
            List<StandingsResponse.ProblemResult> results = labels.stream()
                    .map(label -> {
                        ProblemData pd = d.problems.getOrDefault(label, new ProblemData());
                        return new StandingsResponse.ProblemResult(
                                label, pd.solved, pd.attempts, pd.solvedAtMinute);
                    })
                    .toList();

            entries.add(new StandingsResponse.StandingsEntry(
                    i + 1, d.userId, d.username, d.solvedCount, d.penaltyMinutes, results));
        }

        return new StandingsResponse(contest.getId(), contest.getTitle(), entries);
    }

    private static class UserStandingsData {
        final long userId;
        final String username;
        int solvedCount = 0;
        long penaltyMinutes = 0;
        Map<String, ProblemData> problems = new HashMap<>();

        UserStandingsData(long userId, String username) {
            this.userId = userId;
            this.username = username;
        }
    }

    private static class ProblemData {
        boolean solved = false;
        int attempts = 0;
        LocalDateTime solvedAt;
        Long solvedAtMinute;
    }
}
