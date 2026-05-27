package com.englishplatform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "test_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @Column(name = "total_count", nullable = false)
    private int totalCount;

    @Column(name = "correct_count", nullable = false)
    private int correctCount;

    @Column(name = "score_percent", nullable = false)
    private BigDecimal scorePercent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TestDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "word_filter_type", nullable = false)
    private WordFilterType wordFilterType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filter_lesson_id")
    private Lesson filterLesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filter_group_id")
    private Group filterGroup;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TestResult> results = new ArrayList<>();

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        completedAt = LocalDateTime.now();
    }
}
