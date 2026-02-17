package com.codearena.problem.repository;

import com.codearena.problem.entity.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    List<TestCase> findByProblemIdOrderByOrderIndex(Long problemId);

    List<TestCase> findByProblemIdAndIsSampleTrueOrderByOrderIndex(Long problemId);
}
