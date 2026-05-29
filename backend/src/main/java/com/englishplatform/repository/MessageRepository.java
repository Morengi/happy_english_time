package com.englishplatform.repository;

import com.englishplatform.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE " +
           "(m.sender.id = :userId AND m.receiver.id = :otherId) OR " +
           "(m.sender.id = :otherId AND m.receiver.id = :userId) " +
           "ORDER BY m.createdAt ASC")
    List<Message> findPrivateConversation(@Param("userId") Long userId,
                                           @Param("otherId") Long otherId);

    @Query("SELECT m FROM Message m WHERE m.group.id = :groupId ORDER BY m.createdAt ASC")
    List<Message> findGroupMessages(@Param("groupId") Long groupId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver.id = :userId AND m.isRead = false")
    long countUnreadForUser(@Param("userId") Long userId);

    @Query("SELECT m.sender.id, COUNT(m) FROM Message m WHERE m.receiver.id = :userId AND m.isRead = false GROUP BY m.sender.id")
    List<Object[]> countUnreadPerSender(@Param("userId") Long userId);

    @Transactional
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.receiver.id = :userId AND m.sender.id = :senderId")
    void markAsRead(@Param("userId") Long userId, @Param("senderId") Long senderId);

    @Query("SELECT m FROM Message m WHERE " +
           "((m.sender.id = :userId AND m.receiver IS NOT NULL) OR " +
           "(m.receiver.id = :userId)) " +
           "ORDER BY m.createdAt DESC")
    List<Message> findAllPersonalForUser(@Param("userId") Long userId);
}
