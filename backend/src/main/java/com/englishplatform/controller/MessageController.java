package com.englishplatform.controller;

import com.englishplatform.dto.request.MessageRequest;
import com.englishplatform.dto.response.MessageResponse;
import com.englishplatform.dto.response.UserResponse;
import com.englishplatform.entity.User;
import com.englishplatform.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(@Valid @RequestBody MessageRequest req,
                                                        @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(messageService.sendMessage(req, user));
    }

    @GetMapping("/private/{otherId}")
    public ResponseEntity<List<MessageResponse>> getPrivateConversation(
            @PathVariable Long otherId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(messageService.getPrivateConversation(user.getId(), otherId));
    }

    @GetMapping("/contacts")
    public ResponseEntity<List<UserResponse>> getContacts(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(messageService.getConversationPartners(user));
    }

    @GetMapping("/unread")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("count", messageService.countUnread(user)));
    }

    @GetMapping("/unread-by-sender")
    public ResponseEntity<Map<Long, Long>> getUnreadBySender(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(messageService.countUnreadPerSender(user));
    }
}
