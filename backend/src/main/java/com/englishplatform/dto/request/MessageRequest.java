package com.englishplatform.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MessageRequest {
    private Long receiverId;
    private Long groupId;
    @NotBlank
    private String content;
}
