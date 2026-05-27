package com.englishplatform.dto.response;

import com.englishplatform.entity.Message;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageResponse {
    private Long id;
    private UserResponse sender;
    private Long receiverId;
    private Long groupId;
    private String content;
    private boolean isRead;
    private LocalDateTime createdAt;

    public static MessageResponse from(Message message) {
        MessageResponse r = new MessageResponse();
        r.setId(message.getId());
        r.setSender(UserResponse.from(message.getSender()));
        if (message.getReceiver() != null) r.setReceiverId(message.getReceiver().getId());
        if (message.getGroup() != null) r.setGroupId(message.getGroup().getId());
        r.setContent(message.getContent());
        r.setRead(message.isRead());
        r.setCreatedAt(message.getCreatedAt());
        return r;
    }
}
