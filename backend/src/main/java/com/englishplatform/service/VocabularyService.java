package com.englishplatform.service;

import com.englishplatform.dto.request.WordRequest;
import com.englishplatform.dto.response.WordResponse;
import com.englishplatform.entity.*;
import com.englishplatform.repository.GroupRepository;
import com.englishplatform.repository.LessonRepository;
import com.englishplatform.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VocabularyService {

    private final WordRepository wordRepository;
    private final LessonRepository lessonRepository;
    private final GroupRepository groupRepository;

    @Transactional
    public WordResponse addWord(WordRequest req, User user) {
        // Check for duplicate
        wordRepository.findDuplicate(
                user.getId(), req.getEnglishWord(), req.getSourceType(),
                req.getLessonId(), req.getGroupId()
        ).ifPresent(w -> { throw new RuntimeException("Word already exists in vocabulary"); });

        Lesson lesson = null;
        Group group = null;
        if (req.getLessonId() != null) {
            lesson = lessonRepository.findById(req.getLessonId()).orElse(null);
        }
        if (req.getGroupId() != null) {
            group = groupRepository.findById(req.getGroupId()).orElse(null);
        }

        Word word = Word.builder()
                .englishWord(req.getEnglishWord().trim())
                .russianTranslation(req.getRussianTranslation().trim())
                .user(user)
                .sourceType(req.getSourceType())
                .lesson(lesson)
                .group(group)
                .build();
        return WordResponse.from(wordRepository.save(word));
    }

    @Transactional
    public WordResponse updateWord(Long id, WordRequest req, User user) {
        Word word = wordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Word not found"));
        if (!word.getUser().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new RuntimeException("Access denied");
        }
        word.setEnglishWord(req.getEnglishWord().trim());
        word.setRussianTranslation(req.getRussianTranslation().trim());
        return WordResponse.from(wordRepository.save(word));
    }

    @Transactional
    public void deleteWord(Long id, User user) {
        Word word = wordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Word not found"));
        if (!word.getUser().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new RuntimeException("Access denied");
        }
        wordRepository.delete(word);
    }

    public List<WordResponse> getVocabulary(User user, String sourceFilter) {
        List<Word> words;
        if (sourceFilter == null || sourceFilter.isBlank()) {
            words = wordRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        } else {
            try {
                WordSourceType type = WordSourceType.valueOf(sourceFilter.toUpperCase());
                words = wordRepository.findByUserIdAndSourceTypeOrderByCreatedAtDesc(user.getId(), type);
            } catch (IllegalArgumentException e) {
                words = wordRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
            }
        }
        return words.stream().map(WordResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public void addWordsBulk(List<WordRequest> requests, User user) {
        for (WordRequest req : requests) {
            try {
                addWord(req, user);
            } catch (RuntimeException e) {
                // Skip duplicates silently in bulk operations
            }
        }
    }

    public List<Word> getWordsForTest(User user, WordFilterType filterType,
                                       Long filterLessonId, Long filterGroupId) {
        return switch (filterType) {
            case ALL -> wordRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
            case BY_LESSON -> {
                if (filterLessonId != null) {
                    yield wordRepository.findByUserIdAndLessonId(user.getId(), filterLessonId);
                } else {
                    yield wordRepository.findAllLessonWordsByUserId(user.getId());
                }
            }
            case BY_GROUP -> {
                if (filterGroupId != null) {
                    yield wordRepository.findByUserIdAndGroupId(user.getId(), filterGroupId);
                } else {
                    yield wordRepository.findAllGroupWordsByUserId(user.getId());
                }
            }
        };
    }
}
