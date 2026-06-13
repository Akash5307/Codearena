package com.codearena.contest.repository;

import com.codearena.contest.entity.Contest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContestRepository extends JpaRepository<Contest, Long> {

    Optional<Contest> findBySlug(String slug);

    boolean existsBySlug(String slug);

    // Rated contests not yet processed by the rating engine (filtered to ENDED in code,
    // since contest state is computed from the clock, not stored).
    List<Contest> findByIsRatedTrueAndRatingsAppliedFalse();

    // Upcoming: start_time > now
    @Query("SELECT c FROM Contest c WHERE c.startTime > :now ORDER BY c.startTime ASC")
    Page<Contest> findUpcoming(@Param("now") LocalDateTime now, Pageable pageable);

    // Running: start_time <= now AND start_time + duration > now
    @Query("SELECT c FROM Contest c WHERE c.startTime <= :now AND " +
            "FUNCTION('TIMESTAMPADD', MINUTE, c.durationMinutes, c.startTime) > :now " +
            "ORDER BY c.startTime DESC")
    Page<Contest> findRunning(@Param("now") LocalDateTime now, Pageable pageable);

    // Past: start_time + duration <= now
    @Query("SELECT c FROM Contest c WHERE " +
            "FUNCTION('TIMESTAMPADD', MINUTE, c.durationMinutes, c.startTime) <= :now " +
            "ORDER BY c.startTime DESC")
    Page<Contest> findPast(@Param("now") LocalDateTime now, Pageable pageable);
}
