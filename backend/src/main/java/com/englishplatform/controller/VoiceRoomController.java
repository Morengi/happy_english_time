package com.englishplatform.controller;

import com.englishplatform.dto.request.VoiceRoomRequest;
import com.englishplatform.dto.response.VoiceParticipantResponse;
import com.englishplatform.dto.response.VoiceRoomResponse;
import com.englishplatform.entity.Role;
import com.englishplatform.entity.User;
import com.englishplatform.service.VoiceRoomService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/voice-rooms")
public class VoiceRoomController {

    private final VoiceRoomService voiceRoomService;

    public VoiceRoomController(VoiceRoomService voiceRoomService) {
        this.voiceRoomService = voiceRoomService;
    }

    /** List rooms visible to the current user. */
    @GetMapping
    public ResponseEntity<List<VoiceRoomResponse>> getRooms(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(voiceRoomService.getRoomsForUser(user));
    }

    /** Create a new room (TEACHER or ADMIN only). */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ADMIN')")
    public ResponseEntity<VoiceRoomResponse> createRoom(
            @Valid @RequestBody VoiceRoomRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(voiceRoomService.createRoom(req, user));
    }

    /** End / close a room (creator or ADMIN). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> endRoom(@PathVariable Long id,
                                        @AuthenticationPrincipal User user) {
        voiceRoomService.endRoom(id, user);
        return ResponseEntity.noContent().build();
    }

    /** Get the list of current live participants in a room. */
    @GetMapping("/{id}/participants")
    public ResponseEntity<List<VoiceParticipantResponse>> getParticipants(@PathVariable Long id) {
        return ResponseEntity.ok(voiceRoomService.getParticipants(id));
    }

    /** Join a room. Returns full participant list after joining. */
    @PostMapping("/{id}/join")
    public ResponseEntity<List<VoiceParticipantResponse>> joinRoom(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(voiceRoomService.joinRoom(id, user));
    }

    /** Leave a room. */
    @PostMapping("/{id}/leave")
    public ResponseEntity<Void> leaveRoom(@PathVariable Long id,
                                          @AuthenticationPrincipal User user) {
        voiceRoomService.leaveRoom(id, user);
        return ResponseEntity.noContent().build();
    }
}
