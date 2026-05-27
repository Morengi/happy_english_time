package com.englishplatform.dto.request;

import com.englishplatform.entity.TestDirection;
import com.englishplatform.entity.WordFilterType;
import lombok.Data;

import java.util.List;

@Data
public class TestSubmitRequest {
    private TestDirection direction;
    private WordFilterType filterType;
    private Long filterLessonId;
    private Long filterGroupId;
    private Long groupId;
    private List<TestAnswerRequest> answers;

    @Data
    public static class TestAnswerRequest {
        private Long wordId;
        private String englishWord;
        private String russianTranslation;
        private String userAnswer;
    }
}
