package com.englishplatform.service;

import com.englishplatform.dto.request.MessageRequest;
import com.englishplatform.dto.response.MessageResponse;
import com.englishplatform.dto.response.UserResponse;
import com.englishplatform.entity.Group;
import com.englishplatform.entity.Message;
import com.englishplatform.entity.User;
import com.englishplatform.repository.GroupRepository;
import com.englishplatform.repository.MessageRepository;
import com.englishplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public MessageResponse sendMessage(MessageRequest req, User sender) {
        Message message = Message.builder()
                .sender(sender)
                .content(req.getContent())
                .isRead(false)
                .build();

        if (req.getGroupId() != null) {
            Group group = groupRepository.findById(req.getGroupId())
                    .orElseThrow(() -> new RuntimeException("Group not found"));
            message.setGroup(group);
        } else if (req.getReceiverId() != null) {
            User receiver = userRepository.findById(req.getReceiverId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            message.setReceiver(receiver);
        } else {
            throw new RuntimeException("Either receiverId or groupId must be provided");
        }

        Message saved = messageRepository.save(message);
        MessageResponse response = MessageResponse.from(saved);

        // Broadcast via WebSocket
        if (saved.getGroup() != null) {
            messagingTemplate.convertAndSend("/topic/group/" + saved.getGroup().getId(), response);
        } else {
            messagingTemplate.convertAndSendToUser(
                    saved.getReceiver().getNickname(), "/queue/messages", response);
            messagingTemplate.convertAndSendToUser(
                    sender.getNickname(), "/queue/messages", response);
        }

        return response;
    }

    public List<MessageResponse> getPrivateConversation(Long userId, Long otherId) {
        messageRepository.markAsRead(userId, otherId);
        return messageRepository.findPrivateConversation(userId, otherId)
                .stream().map(MessageResponse::from).collect(Collectors.toList());
    }

    public List<MessageResponse> getGroupMessages(Long groupId) {
        return messageRepository.findGroupMessages(groupId)
                .stream().map(MessageResponse::from).collect(Collectors.toList());
    }

    public List<UserResponse> getConversationPartners(User user) {
        List<Message> allPersonal = messageRepository.findAllPersonalForUser(user.getId());
        Set<Long> partnerIds = new HashSet<>();
        for (Message m : allPersonal) {
            if (m.getSender() != null && !m.getSender().getId().equals(user.getId())) {
                partnerIds.add(m.getSender().getId());
            }
            if (m.getReceiver() != null && !m.getReceiver().getId().equals(user.getId())) {
                partnerIds.add(m.getReceiver().getId());
            }
        }
        return partnerIds.stream()
                .map(id -> userRepository.findById(id).map(UserResponse::from).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public long countUnread(User user) {
        return messageRepository.countUnreadForUser(user.getId());
    }
}
