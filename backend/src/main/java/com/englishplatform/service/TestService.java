package com.englishplatform.service;

import com.englishplatform.dto.request.TestSubmitRequest;
import com.englishplatform.dto.response.RankingResponse;
import com.englishplatform.dto.response.TestSessionResponse;
import com.englishplatform.dto.response.WordResponse;
import com.englishplatform.entity.*;
import com.englishplatform.repository.GroupRepository;
import com.englishplatform.repository.LessonRepository;
import com.englishplatform.repository.TestSessionRepository;
import com.englishplatform.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TestService {

    private final TestSessionRepository testSessionRepository;
    private final VocabularyService vocabularyService;
    private final GroupRepository groupRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;

    public TestService(TestSessionRepository testSessionRepository, VocabularyService vocabularyService,
                       GroupRepository groupRepository, LessonRepository lessonRepository,
                       UserRepository userRepository) {
        this.testSessionRepository = testSessionRepository;
        this.vocabularyService = vocabularyService;
        this.groupRepository = groupRepository;
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
    }

    public List<WordResponse> getTestWords(User user, int count,
                                            WordFilterType filterType,
                                            Long filterLessonId,
                                            Long filterGroupId,
                                            TestDirection direction) {
        List<Word> words = vocabularyService.getWordsForTest(user, filterType, filterLessonId, filterGroupId);

        if (words.isEmpty()) return List.of();

        List<Word> shuffled = new ArrayList<>(words);
        Collections.shuffle(shuffled);

        // count <= 0 means "all words"; also cap to avoid repeating words
        int take = (count <= 0) ? shuffled.size() : Math.min(count, shuffled.size());
        return shuffled.subList(0, take).stream().map(WordResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public TestSessionResponse submitTest(TestSubmitRequest req, User user) {
        int correctCount = 0;
        List<TestResult> results = new ArrayList<>();

        for (TestSubmitRequest.TestAnswerRequest answer : req.getAnswers()) {
            boolean correct = checkAnswer(answer, req.getDirection());
            if (correct) correctCount++;

            TestResult result = TestResult.builder()
                    .englishWord(answer.getEnglishWord())
                    .russianTranslation(answer.getRussianTranslation())
                    .userAnswer(answer.getUserAnswer())
                    .isCorrect(correct)
                    .build();
            results.add(result);
        }

        int total = req.getAnswers().size();
        BigDecimal score = total > 0
                ? BigDecimal.valueOf(correctCount * 100.0 / total).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Group group = null;
        if (req.getGroupId() != null) {
            group = groupRepository.findById(req.getGroupId()).orElse(null);
        }

        Lesson filterLesson = null;
        if (req.getFilterLessonId() != null) {
            filterLesson = lessonRepository.findById(req.getFilterLessonId()).orElse(null);
        }

        Group filterGroup = null;
        if (req.getFilterGroupId() != null) {
            filterGroup = groupRepository.findById(req.getFilterGroupId()).orElse(null);
        }

        TestSession session = TestSession.builder()
                .user(user)
                .group(group)
                .filterLesson(filterLesson)
                .filterGroup(filterGroup)
                .totalCount(total)
                .correctCount(correctCount)
                .scorePercent(score)
                .direction(req.getDirection())
                .wordFilterType(req.getFilterType())
                .build();

        results.forEach(r -> r.setSession(session));
        session.setResults(results);

        TestSession saved = testSessionRepository.save(session);
        return TestSessionResponse.from(saved);
    }

    private boolean checkAnswer(TestSubmitRequest.TestAnswerRequest answer, TestDirection direction) {
        String userAnswer = answer.getUserAnswer();
        if (userAnswer == null || userAnswer.isBlank()) return false;
        userAnswer = userAnswer.trim().toLowerCase();

        if (direction == TestDirection.EN_TO_RU) {
            String[] translations = answer.getRussianTranslation().split("[,;]+");
            for (String t : translations) {
                if (t.trim().toLowerCase().equals(userAnswer)) return true;
            }
            return false;
        } else {
            String[] englishVariants = answer.getEnglishWord().split("[,;]+");
            for (String e : englishVariants) {
                if (e.trim().toLowerCase().equals(userAnswer)) return true;
            }
            return false;
        }
    }

    public List<RankingResponse> getGroupRanking(Long groupId) {
        List<Object[]> raw = testSessionRepository.findGroupRanking(groupId);
        List<RankingResponse> ranking = new ArrayList<>();
        for (Object[] row : raw) {
            Long userId = ((Number) row[0]).longValue();
            BigDecimal avgScore = new BigDecimal(row[1].toString()).setScale(2, RoundingMode.HALF_UP);
            userRepository.findById(userId).ifPresent(u ->
                ranking.add(new RankingResponse(u.getId(), u.getFullName(), u.getNickname(), avgScore, ranking.size() + 1))
            );
        }
        return ranking;
    }

    public List<TestSessionResponse> getUserHistory(User user) {
        return testSessionRepository.findByUserIdOrderByCompletedAtDesc(user.getId())
                .stream().map(TestSessionResponse::from).collect(Collectors.toList());
    }
}
