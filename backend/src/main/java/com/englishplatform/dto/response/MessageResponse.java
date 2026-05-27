package com.englishplatform.dto.response;

import com.englishplatform.entity.Message;

import java.time.LocalDateTime;

public class MessageResponse {
    private Long id;
    private UserResponse sender;
    private Long receiverId;
    private Long groupId;
    private String content;
    private boolean isRead;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public UserResponse getSender() { return sender; }
    public void setSender(UserResponse v) { this.sender = v; }
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long v) { this.receiverId = v; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long v) { this.groupId = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean v) { this.isRead = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }

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
