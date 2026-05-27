package com.englishplatform.repository;

import com.englishplatform.entity.Group;
import com.englishplatform.entity.Lesson;
import com.englishplatform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByTeacher(User teacher);

    @Query("SELECT DISTINCT l FROM Lesson l JOIN l.accessGroups g WHERE g IN :groups")
    List<Lesson> findAccessibleByGroups(@Param("groups") List<Group> groups);

    @Query("SELECT DISTINCT l FROM Lesson l WHERE l.teacher = :teacher OR " +
           "EXISTS (SELECT 1 FROM l.accessGroups g WHERE g IN :groups)")
    List<Lesson> findAllForUser(@Param("teacher") User teacher,
                                @Param("groups") List<Group> groups);
}
