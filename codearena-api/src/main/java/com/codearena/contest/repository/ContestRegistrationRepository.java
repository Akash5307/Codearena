package com.codearena.contest.repository;

import com.codearena.contest.entity.ContestRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContestRegistrationRepository extends JpaRepository<ContestRegistration, Long> {

    boolean existsByContestIdAndUserId(Long contestId, Long userId);

    List<ContestRegistration> findByContestId(Long contestId);

    long countByContestId(Long contestId);
}
