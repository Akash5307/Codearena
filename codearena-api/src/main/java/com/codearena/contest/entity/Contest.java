package com.codearena.contest.entity;

import com.codearena.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contests")
@Getter
@Setter
@NoArgsConstructor
public class Contest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ContestType type;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "is_rated", nullable = false)
    private Boolean isRated = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @OneToMany(mappedBy = "contest", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<ContestProblem> contestProblems = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getEndTime() {
        return startTime.plusMinutes(durationMinutes);
    }

    public ContestState getState() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startTime)) {
            return ContestState.BEFORE;
        } else if (now.isBefore(getEndTime())) {
            return ContestState.RUNNING;
        } else {
            return ContestState.ENDED;
        }
    }

    public enum ContestType {
        ICPC, IOI, EDUCATIONAL
    }

    public enum ContestState {
        BEFORE, RUNNING, ENDED
    }
}
