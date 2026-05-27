package com.englishplatform.service;

import com.englishplatform.dto.request.TestSubmitRequest;
import com.englishplatform.dto.response.RankingResponse;
import com.englishplatform.dto.response.TestSessionResponse;
import com.englishplatform.dto.response.WordResponse;
import com.englishplatform.entity.*;
import com.englishplatform.repository.GroupRepository;
import com.englishplatform.repository.TestSessionRepository;
import com.englishplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestService {

    private final TestSessionRepository testSessionRepository;
    private final VocabularyService vocabularyService;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public List<WordResponse> getTestWords(User user, int count,
                                            WordFilterType filterType,
                                            Long filterLessonId,
                                            Long filterGroupId,
                                            TestDirection direction) {
        List<Word> words = vocabularyService.getWordsForTest(user, filterType, filterLessonId, filterGroupId);

        if (words.isEmpty()) return List.of();

        // Shuffle and limit; allow repeats if count > available
        List<Word> shuffled = new ArrayList<>(words);
        Collections.shuffle(shuffled);

        List<Word> testWords = new ArrayList<>();
        int idx = 0;
        for (int i = 0; i < count; i++) {
            testWords.add(shuffled.get(idx % shuffled.size()));
            idx++;
        }

        return testWords.stream().map(WordResponse::from).collect(Collectors.toList());
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

        TestSession session = TestSession.builder()
                .user(user)
                .group(group)
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
            // Check against all comma/semicolon-separated Russian translations
            String[] translations = answer.getRussianTranslation().split("[,;]+");
            for (String t : translations) {
                if (t.trim().toLowerCase().equals(userAnswer)) return true;
            }
            return false;
        } else {
            // RU_TO_EN: check English word
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
        for (int i = 0; i < raw.size(); i++) {
            Object[] row = raw.get(i);
            Long userId = ((Number) row[0]).longValue();
            BigDecimal avgScore = new BigDecimal(row[1].toString()).setScale(2, RoundingMode.HALF_UP);
            userRepository.findById(userId).ifPresent(u -> {
                ranking.add(new RankingResponse(u.getId(), u.getFullName(), u.getNickname(), avgScore, ranking.size() + 1));
            });
        }
        return ranking;
    }

    public List<TestSessionResponse> getUserHistory(User user) {
        return testSessionRepository.findByUserIdOrderByCompletedAtDesc(user.getId())
                .stream().map(TestSessionResponse::from).collect(Collectors.toList());
    }
}
