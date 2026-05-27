package com.englishplatform.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "words")
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "english_word", nullable = false)
    private String englishWord;

    @Column(name = "russian_translation", nullable = false)
    private String russianTranslation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private WordSourceType sourceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Word() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEnglishWord() { return englishWord; }
    public void setEnglishWord(String v) { this.englishWord = v; }
    public String getRussianTranslation() { return russianTranslation; }
    public void setRussianTranslation(String v) { this.russianTranslation = v; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public WordSourceType getSourceType() { return sourceType; }
    public void setSourceType(WordSourceType v) { this.sourceType = v; }
    public Lesson getLesson() { return lesson; }
    public void setLesson(Lesson lesson) { this.lesson = lesson; }
    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String englishWord, russianTranslation;
        private User user;
        private WordSourceType sourceType;
        private Lesson lesson;
        private Group group;
        public Builder englishWord(String v) { this.englishWord = v; return this; }
        public Builder russianTranslation(String v) { this.russianTranslation = v; return this; }
        public Builder user(User v) { this.user = v; return this; }
        public Builder sourceType(WordSourceType v) { this.sourceType = v; return this; }
        public Builder lesson(Lesson v) { this.lesson = v; return this; }
        public Builder group(Group v) { this.group = v; return this; }
        public Word build() {
            Word w = new Word();
            w.englishWord = englishWord; w.russianTranslation = russianTranslation;
            w.user = user; w.sourceType = sourceType; w.lesson = lesson; w.group = group;
            return w;
        }
    }

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
