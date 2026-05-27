package com.englishplatform.dto.response;

import com.englishplatform.entity.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class TestSessionResponse {
    private Long id;
    private int totalCount;
    private int correctCount;
    private BigDecimal scorePercent;
    private TestDirection direction;
    private WordFilterType wordFilterType;
    private LocalDateTime completedAt;
    private List<TestResultResponse> results;

    @Data
    public static class TestResultResponse {
        private Long id;
        private Long wordId;
        private String englishWord;
        private String russianTranslation;
        private String userAnswer;
        private boolean isCorrect;
    }

    public static TestSessionResponse from(TestSession session) {
        TestSessionResponse r = new TestSessionResponse();
        r.setId(session.getId());
        r.setTotalCount(session.getTotalCount());
        r.setCorrectCount(session.getCorrectCount());
        r.setScorePercent(session.getScorePercent());
        r.setDirection(session.getDirection());
        r.setWordFilterType(session.getWordFilterType());
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
