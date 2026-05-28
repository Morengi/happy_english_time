package com.englishplatform.websocket;

import com.englishplatform.dto.RoomEventMessage;
import com.englishplatform.dto.SignalMessage;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class VoiceSignalingController {

    private final SimpMessagingTemplate messagingTemplate;

    public VoiceSignalingController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Peer-to-peer WebRTC signal (OFFER / ANSWER / ICE_CANDIDATE / MUTE_*).
     * Client sends to: /app/voice/{roomId}/signal
     * Server forwards to: /user/{toUserNickname}/queue/voice-signal
     */
    @MessageMapping("/voice/{roomId}/signal")
    public void handleSignal(@DestinationVariable Long roomId,
                             @Payload SignalMessage signal,
                             Principal principal) {
        if (signal.getToUserNickname() == null || signal.getToUserNickname().isBlank()) {
            return;
        }
        // Stamp the sender's identity from the authenticated principal
        if (principal != null) {
            signal.setFromNickname(principal.getName());
        }
        signal.setRoomId(roomId);
        messagingTemplate.convertAndSendToUser(
                signal.getToUserNickname(),
                "/queue/voice-signal",
                signal
        );
    }

    /**
     * Room-wide event broadcast (JOIN / LEAVE / custom announcements).
     * Client sends to: /app/voice/{roomId}/event
     * Server broadcasts to: /topic/voice/{roomId}/events
     *
     * Note: authoritative JOIN/LEAVE events are also sent by VoiceRoomService via REST.
     * This channel is available for lightweight client-side announcements.
     */
    @MessageMapping("/voice/{roomId}/event")
    public void handleEvent(@DestinationVariable Long roomId,
                            @Payload RoomEventMessage event,
                            Principal principal) {
        event.setRoomId(roomId);
        if (principal != null && (event.getUserNickname() == null || event.getUserNickname().isBlank())) {
            event.setUserNickname(principal.getName());
        }
        messagingTemplate.convertAndSend("/topic/voice/" + roomId + "/events", event);
    }
}
