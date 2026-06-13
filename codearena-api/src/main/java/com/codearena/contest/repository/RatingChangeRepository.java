package com.codearena.contest.repository;

import com.codearena.contest.entity.RatingChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RatingChangeRepository extends JpaRepository<RatingChange, Long> {

    List<RatingChange> findByUserIdOrderByCreatedAtDesc(Long userId);
}
