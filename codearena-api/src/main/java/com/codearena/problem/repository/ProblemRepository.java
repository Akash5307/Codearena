package com.codearena.problem.repository;

import com.codearena.problem.entity.Problem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long> {

    Optional<Problem> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("""
            SELECT DISTINCT p FROM Problem p
            LEFT JOIN p.tags t
            WHERE p.isPublished = true
            AND (:title IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :title, '%')))
            AND (:difficulty IS NULL OR p.difficulty = :difficulty)
            AND (:tagName IS NULL OR t.name = :tagName)
            """)
    Page<Problem> findPublishedWithFilters(
            @Param("title") String title,
            @Param("difficulty") Problem.Difficulty difficulty,
            @Param("tagName") String tagName,
            Pageable pageable
    );
}
