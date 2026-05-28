package com.englishplatform.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "test_sessions")
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
    private List<TestResult> results = new ArrayList<>();

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    public TestSession() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int v) { this.totalCount = v; }
    public int getCorrectCount() { return correctCount; }
    public void setCorrectCount(int v) { this.correctCount = v; }
    public BigDecimal getScorePercent() { return scorePercent; }
    public void setScorePercent(BigDecimal v) { this.scorePercent = v; }
    public TestDirection getDirection() { return direction; }
    public void setDirection(TestDirection v) { this.direction = v; }
    public WordFilterType getWordFilterType() { return wordFilterType; }
    public void setWordFilterType(WordFilterType v) { this.wordFilterType = v; }
    public Lesson getFilterLesson() { return filterLesson; }
    public void setFilterLesson(Lesson v) { this.filterLesson = v; }
    public Group getFilterGroup() { return filterGroup; }
    public void setFilterGroup(Group v) { this.filterGroup = v; }
    public List<TestResult> getResults() { return results; }
    public void setResults(List<TestResult> results) { this.results = results; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime v) { this.completedAt = v; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private User user; private Group group;
        private Lesson filterLesson; private Group filterGroup;
        private int totalCount, correctCount;
        private BigDecimal scorePercent;
        private TestDirection direction;
        private WordFilterType wordFilterType;
        public Builder user(User v) { this.user = v; return this; }
        public Builder group(Group v) { this.group = v; return this; }
        public Builder filterLesson(Lesson v) { this.filterLesson = v; return this; }
        public Builder filterGroup(Group v) { this.filterGroup = v; return this; }
        public Builder totalCount(int v) { this.totalCount = v; return this; }
        public Builder correctCount(int v) { this.correctCount = v; return this; }
        public Builder scorePercent(BigDecimal v) { this.scorePercent = v; return this; }
        public Builder direction(TestDirection v) { this.direction = v; return this; }
        public Builder wordFilterType(WordFilterType v) { this.wordFilterType = v; return this; }
        public TestSession build() {
            TestSession s = new TestSession();
            s.user = user; s.group = group;
            s.filterLesson = filterLesson; s.filterGroup = filterGroup;
            s.totalCount = totalCount; s.correctCount = correctCount;
            s.scorePercent = scorePercent;
            s.direction = direction; s.wordFilterType = wordFilterType;
            return s;
        }
    }

    @PrePersist
    protected void onCreate() { completedAt = LocalDateTime.now(); }
}
