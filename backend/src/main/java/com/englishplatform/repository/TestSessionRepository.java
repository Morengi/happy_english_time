package com.englishplatform.repository;

import com.englishplatform.entity.TestSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TestSessionRepository extends JpaRepository<TestSession, Long> {

    List<TestSession> findByUserIdOrderByCompletedAtDesc(Long userId);

    @Query("SELECT ts FROM TestSession ts WHERE ts.user.id = :userId ORDER BY ts.completedAt DESC")
    List<TestSession> findRecentByUserId(@Param("userId") Long userId,
                                          org.springframework.data.domain.Pageable pageable);

    @Query("SELECT ts FROM TestSession ts WHERE ts.group.id = :groupId ORDER BY ts.completedAt DESC")
    List<TestSession> findByGroupId(@Param("groupId") Long groupId);

    @Query("SELECT ts.user.id, AVG(ts.scorePercent) as avgScore " +
           "FROM TestSession ts " +
           "WHERE ts.group.id = :groupId " +
           "GROUP BY ts.user.id " +
           "ORDER BY avgScore DESC")
    List<Object[]> findGroupRanking(@Param("groupId") Long groupId);
}
