package com.englishplatform.dto.response;

import com.englishplatform.entity.TestDirection;
import com.englishplatform.entity.TestSession;
import com.englishplatform.entity.WordFilterType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class TestSessionResponse {
    private Long id;
    private int totalCount;
    private int correctCount;
    private BigDecimal scorePercent;
    private TestDirection direction;
    private WordFilterType wordFilterType;
    // Filter details
    private Long filterGroupId;
    private String filterGroupName;
    private Long filterLessonId;
    private String filterLessonTitle;
    private LocalDateTime completedAt;
    private List<TestResultResponse> results;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
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
    public Long getFilterGroupId() { return filterGroupId; }
    public void setFilterGroupId(Long v) { this.filterGroupId = v; }
    public String getFilterGroupName() { return filterGroupName; }
    public void setFilterGroupName(String v) { this.filterGroupName = v; }
    public Long getFilterLessonId() { return filterLessonId; }
    public void setFilterLessonId(Long v) { this.filterLessonId = v; }
    public String getFilterLessonTitle() { return filterLessonTitle; }
    public void setFilterLessonTitle(String v) { this.filterLessonTitle = v; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime v) { this.completedAt = v; }
    public List<TestResultResponse> getResults() { return results; }
    public void setResults(List<TestResultResponse> v) { this.results = v; }

    public static class TestResultResponse {
        private Long id;
        private Long wordId;
        private String englishWord;
        private String russianTranslation;
        private String userAnswer;
        private boolean isCorrect;

        public Long getId() { return id; }
        public void setId(Long v) { this.id = v; }
        public Long getWordId() { return wordId; }
        public void setWordId(Long v) { this.wordId = v; }
        public String getEnglishWord() { return englishWord; }
        public void setEnglishWord(String v) { this.englishWord = v; }
        public String getRussianTranslation() { return russianTranslation; }
        public void setRussianTranslation(String v) { this.russianTranslation = v; }
        public String getUserAnswer() { return userAnswer; }
        public void setUserAnswer(String v) { this.userAnswer = v; }
        public boolean isCorrect() { return isCorrect; }
        public void setCorrect(boolean v) { this.isCorrect = v; }
    }

    public static TestSessionResponse from(TestSession session) {
        TestSessionResponse r = new TestSessionResponse();
        r.setId(session.getId());
        r.setTotalCount(session.getTotalCount());
        r.setCorrectCount(session.getCorrectCount());
        r.setScorePercent(session.getScorePercent());
        r.setDirection(session.getDirection());
        r.setWordFilterType(session.getWordFilterType());
        if (session.getFilterGroup() != null) {
            r.setFilterGroupId(session.getFilterGroup().getId());
            r.setFilterGroupName(session.getFilterGroup().getName());
        }
        if (session.getFilterLesson() != null) {
            r.setFilterLessonId(session.getFilterLesson().getId());
            r.setFilterLessonTitle(session.getFilterLesson().getTitle());
        }
        r.setCompletedAt(session.getCompletedAt());
        r.setResults(session.getResults().stream().map(tr -> {
            TestResultResponse trr = new TestResultResponse();
            trr.setId(tr.getId());
            if (tr.getWord() != null) trr.setWordId(tr.getWord().getId());
            trr.setEnglishWord(tr.getEnglishWord());
            trr.setRussianTranslation(tr.getRussianTranslation());
            trr.setUserAnswer(tr.getUserAnswer());
            trr.setCorrect(tr.isCorrect());
            return trr;
        }).collect(Collectors.toList()));
        return r;
    }
}
