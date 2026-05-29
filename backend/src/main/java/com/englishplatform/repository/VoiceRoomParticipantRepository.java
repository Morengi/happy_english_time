package com.englishplatform.repository;

import com.englishplatform.entity.User;
import com.englishplatform.entity.VoiceRoom;
import com.englishplatform.entity.VoiceRoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VoiceRoomParticipantRepository extends JpaRepository<VoiceRoomParticipant, Long> {

    /** All users currently in the room (leftAt IS NULL). */
    List<VoiceRoomParticipant> findByRoomAndLeftAtIsNull(VoiceRoom room);

    /** Find an active (not-yet-left) entry for a specific user in a room. */
    Optional<VoiceRoomParticipant> findByRoomAndUserAndLeftAtIsNull(VoiceRoom room, User user);

    /** Count current live participants. */
    long countByRoomAndLeftAtIsNull(VoiceRoom room);

    /** Find the most recent participant record for a user in a room (active OR past). */
    Optional<VoiceRoomParticipant> findFirstByRoomAndUserOrderByJoinedAtDesc(VoiceRoom room, User user);

    /** Mark all live participants as left when a room is ended. */
    @Modifying
    @Query("UPDATE VoiceRoomParticipant p SET p.leftAt = CURRENT_TIMESTAMP WHERE p.room = :room AND p.leftAt IS NULL")
    void leaveAllInRoom(@Param("room") VoiceRoom room);
}
