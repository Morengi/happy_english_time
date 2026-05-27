package com.englishplatform.repository;

import com.englishplatform.entity.Word;
import com.englishplatform.entity.WordSourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WordRepository extends JpaRepository<Word, Long> {

    List<Word> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Word> findByUserIdAndSourceTypeOrderByCreatedAtDesc(Long userId, WordSourceType sourceType);

    @Query("SELECT w FROM Word w WHERE w.user.id = :userId AND w.lesson.id = :lessonId")
    List<Word> findByUserIdAndLessonId(@Param("userId") Long userId, @Param("lessonId") Long lessonId);

    @Query("SELECT w FROM Word w WHERE w.user.id = :userId AND w.group.id = :groupId")
    List<Word> findByUserIdAndGroupId(@Param("userId") Long userId, @Param("groupId") Long groupId);

    @Query("SELECT w FROM Word w WHERE w.user.id = :userId AND w.sourceType = 'LESSON'")
    List<Word> findAllLessonWordsByUserId(@Param("userId") Long userId);

    @Query("SELECT w FROM Word w WHERE w.user.id = :userId AND w.sourceType = 'GROUP'")
    List<Word> findAllGroupWordsByUserId(@Param("userId") Long userId);

    @Query("SELECT w FROM Word w WHERE w.user.id = :userId " +
           "AND w.englishWord = :englishWord " +
           "AND w.sourceType = :sourceType " +
           "AND (:lessonId IS NULL OR w.lesson.id = :lessonId) " +
           "AND (:groupId IS NULL OR w.group.id = :groupId)")
    Optional<Word> findDuplicate(@Param("userId") Long userId,
                                  @Param("englishWord") String englishWord,
                                  @Param("sourceType") WordSourceType sourceType,
                                  @Param("lessonId") Long lessonId,
                                  @Param("groupId") Long groupId);

    @Query("SELECT COUNT(w) FROM Word w WHERE w.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);
}
