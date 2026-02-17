package com.codearena.submission.repository;

import com.codearena.submission.entity.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    Page<Submission> findAllByOrderBySubmittedAtDesc(Pageable pageable);

    @Query("""
            SELECT s FROM Submission s
            WHERE (:userId IS NULL OR s.user.id = :userId)
            AND (:problemId IS NULL OR s.problem.id = :problemId)
            AND (:language IS NULL OR s.language = :language)
            AND (:verdict IS NULL OR s.verdict = :verdict)
            ORDER BY s.submittedAt DESC
            """)
    Page<Submission> findWithFilters(
            @Param("userId") Long userId,
            @Param("problemId") Long problemId,
            @Param("language") Submission.Language language,
            @Param("verdict") Submission.Verdict verdict,
            Pageable pageable
    );

    Page<Submission> findByUserUsernameOrderBySubmittedAtDesc(String username, Pageable pageable);

    Page<Submission> findByContestIdAndUserIdOrderBySubmittedAtDesc(Long contestId, Long userId, Pageable pageable);

    @Query("SELECT COUNT(s) > 0 FROM Submission s WHERE s.user.id = :userId AND s.submittedAt > :since")
    boolean existsRecentByUserId(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
