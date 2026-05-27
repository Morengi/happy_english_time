package com.englishplatform.dto.response;

import com.englishplatform.entity.Word;
import com.englishplatform.entity.WordSourceType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
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
