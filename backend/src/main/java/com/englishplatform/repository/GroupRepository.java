package com.englishplatform.repository;

import com.englishplatform.entity.Group;
import com.englishplatform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findByTeacher(User teacher);

    @Query("SELECT g FROM Group g JOIN g.members m WHERE m = :user")
    List<Group> findByMember(@Param("user") User user);

    @Query("SELECT g FROM Group g WHERE g.teacher = :user OR :user MEMBER OF g.members")
    List<Group> findAllForUser(@Param("user") User user);
}
