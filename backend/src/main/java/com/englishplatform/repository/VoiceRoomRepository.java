package com.englishplatform.repository;

import com.englishplatform.entity.User;
import com.englishplatform.entity.VoiceRoom;
import com.englishplatform.entity.VoiceRoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VoiceRoomRepository extends JpaRepository<VoiceRoom, Long> {

    List<VoiceRoom> findByStatus(VoiceRoomStatus status);

    List<VoiceRoom> findByCreatorAndStatus(User creator, VoiceRoomStatus status);

    /**
     * Returns active rooms whose creator shares at least one group with the given student.
     */
    @Query("""
        SELECT DISTINCT r FROM VoiceRoom r
        WHERE r.status = 'ACTIVE'
          AND r.creator.id IN (
              SELECT g.teacher.id FROM Group g
              JOIN g.members m WHERE m.id = :studentId
          )
        """)
    List<VoiceRoom> findActiveRoomsForStudent(@Param("studentId") Long studentId);
}
