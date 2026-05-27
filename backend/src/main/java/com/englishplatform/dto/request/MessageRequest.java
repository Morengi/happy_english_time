package com.englishplatform.dto.request;

import jakarta.validation.constraints.NotBlank;

public class MessageRequest {
    private Long receiverId;
    private Long groupId;
    @NotBlank
    private String content;

    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long v) { this.receiverId = v; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long v) { this.groupId = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
}
