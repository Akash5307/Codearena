package com.codearena.submission.entity;

import com.codearena.contest.entity.Contest;
import com.codearena.problem.entity.Problem;
import com.codearena.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "submissions")
@Getter
@Setter
@NoArgsConstructor
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id")
    private Contest contest;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Language language;

    @Column(name = "source_code", nullable = false, columnDefinition = "TEXT")
    private String sourceCode;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private Verdict verdict = Verdict.PENDING;

    @Column(name = "time_used_ms")
    private Integer timeUsedMs;

    @Column(name = "memory_used_kb")
    private Integer memoryUsedKb;

    @Column(name = "test_cases_passed", nullable = false)
    private Integer testCasesPassed = 0;

    @Column(name = "total_test_cases", nullable = false)
    private Integer totalTestCases = 0;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @Column(name = "judged_at")
    private LocalDateTime judgedAt;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
    }

    public enum Language {
        JAVA, CPP, PYTHON, C, JAVASCRIPT, GO, RUST, KOTLIN
    }

    public enum Verdict {
        PENDING, JUDGING, AC, WA, TLE, MLE, RE, CE
    }
}
