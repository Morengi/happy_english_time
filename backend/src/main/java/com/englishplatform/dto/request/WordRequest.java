package com.englishplatform.dto.request;

import com.englishplatform.entity.WordSourceType;
import jakarta.validation.constraints.NotBlank;

public class WordRequest {
    @NotBlank
    private String englishWord;
    @NotBlank
    private String russianTranslation;
    private WordSourceType sourceType = WordSourceType.PERSONAL;
    private Long lessonId;
    private Long groupId;

    public String getEnglishWord() { return englishWord; }
    public void setEnglishWord(String v) { this.englishWord = v; }
    public String getRussianTranslation() { return russianTranslation; }
    public void setRussianTranslation(String v) { this.russianTranslation = v; }
    public WordSourceType getSourceType() { return sourceType; }
    public void setSourceType(WordSourceType v) { this.sourceType = v; }
    public Long getLessonId() { return lessonId; }
    public void setLessonId(Long v) { this.lessonId = v; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long v) { this.groupId = v; }
}
