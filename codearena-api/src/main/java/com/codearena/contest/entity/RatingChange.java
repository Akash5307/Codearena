package com.codearena.contest.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** One user's rating change from one rated contest — kept for history/auditing. */
@Entity
@Table(name = "rating_changes")
@Getter
@Setter
@NoArgsConstructor
public class RatingChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contest_id", nullable = false)
    private Long contestId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "old_rating", nullable = false)
    private Integer oldRating;

    @Column(name = "new_rating", nullable = false)
    private Integer newRating;

    @Column(nullable = false)
    private Integer delta;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public RatingChange(Long contestId, Long userId, Integer oldRating, Integer newRating, Integer delta) {
        this.contestId = contestId;
        this.userId = userId;
        this.oldRating = oldRating;
        this.newRating = newRating;
        this.delta = delta;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
