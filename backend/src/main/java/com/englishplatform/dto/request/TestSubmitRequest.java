package com.englishplatform.dto.request;

import com.englishplatform.entity.TestDirection;
import com.englishplatform.entity.WordFilterType;

import java.util.List;

public class TestSubmitRequest {
    private TestDirection direction;
    private WordFilterType filterType;
    private Long filterLessonId;
    private Long filterGroupId;
    private Long groupId;
    private List<TestAnswerRequest> answers;

    public TestDirection getDirection() { return direction; }
    public void setDirection(TestDirection v) { this.direction = v; }
    public WordFilterType getFilterType() { return filterType; }
    public void setFilterType(WordFilterType v) { this.filterType = v; }
    public Long getFilterLessonId() { return filterLessonId; }
    public void setFilterLessonId(Long v) { this.filterLessonId = v; }
    public Long getFilterGroupId() { return filterGroupId; }
    public void setFilterGroupId(Long v) { this.filterGroupId = v; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long v) { this.groupId = v; }
    public List<TestAnswerRequest> getAnswers() { return answers; }
    public void setAnswers(List<TestAnswerRequest> v) { this.answers = v; }

    public static class TestAnswerRequest {
        private Long wordId;
        private String englishWord;
        private String russianTranslation;
        private String userAnswer;

        public Long getWordId() { return wordId; }
        public void setWordId(Long v) { this.wordId = v; }
        public String getEnglishWord() { return englishWord; }
        public void setEnglishWord(String v) { this.englishWord = v; }
        public String getRussianTranslation() { return russianTranslation; }
        public void setRussianTranslation(String v) { this.russianTranslation = v; }
        public String getUserAnswer() { return userAnswer; }
        public void setUserAnswer(String v) { this.userAnswer = v; }
    }
}
