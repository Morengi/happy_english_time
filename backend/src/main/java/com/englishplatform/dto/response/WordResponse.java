package com.englishplatform.dto.response;

import com.englishplatform.entity.Word;
import com.englishplatform.entity.WordSourceType;

import java.time.LocalDateTime;

public class WordResponse {
    private Long id;
    private String englishWord;
    private String russianTranslation;
    private WordSourceType sourceType;
    private Long lessonId;
    private String lessonTitle;
    private Long groupId;
    private String groupName;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public String getEnglishWord() { return englishWord; }
    public void setEnglishWord(String v) { this.englishWord = v; }
    public String getRussianTranslation() { return russianTranslation; }
    public void setRussianTranslation(String v) { this.russianTranslation = v; }
    public WordSourceType getSourceType() { return sourceType; }
    public void setSourceType(WordSourceType v) { this.sourceType = v; }
    public Long getLessonId() { return lessonId; }
    public void setLessonId(Long v) { this.lessonId = v; }
    public String getLessonTitle() { return lessonTitle; }
    public void setLessonTitle(String v) { this.lessonTitle = v; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long v) { this.groupId = v; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String v) { this.groupName = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }

    public static WordResponse from(Word word) {
        WordResponse r = new WordResponse();
        r.setId(word.getId());
        r.setEnglishWord(word.getEnglishWord());
        r.setRussianTranslation(word.getRussianTranslation());
        r.setSourceType(word.getSourceType());
        if (word.getLesson() != null) {
            r.setLessonId(word.getLesson().getId());
            r.setLessonTitle(word.getLesson().getTitle());
        }
        if (word.getGroup() != null) {
            r.setGroupId(word.getGroup().getId());
            r.setGroupName(word.getGroup().getName());
        }
        r.setCreatedAt(word.getCreatedAt());
        return r;
    }
}
