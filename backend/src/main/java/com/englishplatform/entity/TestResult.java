package com.englishplatform.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "test_results")
public class TestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private TestSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id")
    private Word word;

    @Column(name = "english_word", nullable = false)
    private String englishWord;

    @Column(name = "russian_translation", nullable = false)
    private String russianTranslation;

    @Column(name = "user_answer")
    private String userAnswer;

    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect;

    public TestResult() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TestSession getSession() { return session; }
    public void setSession(TestSession session) { this.session = session; }
    public Word getWord() { return word; }
    public void setWord(Word word) { this.word = word; }
    public String getEnglishWord() { return englishWord; }
    public void setEnglishWord(String v) { this.englishWord = v; }
    public String getRussianTranslation() { return russianTranslation; }
    public void setRussianTranslation(String v) { this.russianTranslation = v; }
    public String getUserAnswer() { return userAnswer; }
    public void setUserAnswer(String v) { this.userAnswer = v; }
    public boolean isCorrect() { return isCorrect; }
    public void setCorrect(boolean v) { this.isCorrect = v; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private TestSession session; private Word word;
        private String englishWord, russianTranslation, userAnswer;
        private boolean isCorrect;
        public Builder session(TestSession v) { this.session = v; return this; }
        public Builder word(Word v) { this.word = v; return this; }
        public Builder englishWord(String v) { this.englishWord = v; return this; }
        public Builder russianTranslation(String v) { this.russianTranslation = v; return this; }
        public Builder userAnswer(String v) { this.userAnswer = v; return this; }
        public Builder isCorrect(boolean v) { this.isCorrect = v; return this; }
        public TestResult build() {
            TestResult r = new TestResult();
            r.session = session; r.word = word; r.englishWord = englishWord;
            r.russianTranslation = russianTranslation; r.userAnswer = userAnswer;
            r.isCorrect = isCorrect;
            return r;
        }
    }
}
