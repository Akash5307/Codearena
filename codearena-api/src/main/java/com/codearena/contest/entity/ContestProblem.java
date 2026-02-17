package com.codearena.contest.entity;

import com.codearena.problem.entity.Problem;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "contest_problems",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"contest_id", "problem_id"}),
                @UniqueConstraint(columnNames = {"contest_id", "label"})
        })
@Getter
@Setter
@NoArgsConstructor
public class ContestProblem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id", nullable = false)
    private Contest contest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Column(nullable = false, length = 5)
    private String label;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 0;

    @Column
    private Integer points;
}
