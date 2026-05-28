package com.englishplatform.service;

import com.englishplatform.dto.RoomEventMessage;
import com.englishplatform.dto.request.VoiceRoomRequest;
import com.englishplatform.dto.response.VoiceParticipantResponse;
import com.englishplatform.dto.response.VoiceRoomResponse;
import com.englishplatform.entity.*;
import com.englishplatform.repository.VoiceRoomParticipantRepository;
import com.englishplatform.repository.VoiceRoomRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VoiceRoomService {

    private final VoiceRoomRepository voiceRoomRepository;
    private final VoiceRoomParticipantRepository participantRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public VoiceRoomService(VoiceRoomRepository voiceRoomRepository,
                            VoiceRoomParticipantRepository participantRepository,
                            SimpMessagingTemplate messagingTemplate) {
        this.voiceRoomRepository = voiceRoomRepository;
        this.participantRepository = participantRepository;
        this.messagingTemplate = messagingTemplate;
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public VoiceRoomResponse createRoom(VoiceRoomRequest req, User creator) {
        VoiceRoom room = VoiceRoom.builder()
                .name(req.getName())
                .creator(creator)
                .maxParticipants(req.getMaxParticipants())
                .status(VoiceRoomStatus.ACTIVE)
                .build();
        room = voiceRoomRepository.save(room);

        VoiceRoomResponse response = VoiceRoomResponse.from(room, 0);
        messagingTemplate.convertAndSend("/topic/voice-rooms/list", response);
        return response;
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<VoiceRoomResponse> getRoomsForUser(User user) {
        List<VoiceRoom> rooms;
        if (user.getRole() == Role.ADMIN) {
            rooms = voiceRoomRepository.findByStatus(VoiceRoomStatus.ACTIVE);
        } else if (user.getRole() == Role.TEACHER) {
            rooms = voiceRoomRepository.findByCreatorAndStatus(user, VoiceRoomStatus.ACTIVE);
        } else {
            rooms = voiceRoomRepository.findActiveRoomsForStudent(user.getId());
        }
        return rooms.stream()
                .map(r -> VoiceRoomResponse.from(r, (int) participantRepository.countByRoomAndLeftAtIsNull(r)))
                .collect(Collectors.toList());
    }

    public List<VoiceParticipantResponse> getParticipants(Long roomId) {
        VoiceRoom room = findActiveRoom(roomId);
        return participantRepository.findByRoomAndLeftAtIsNull(room).stream()
                .map(VoiceParticipantResponse::from)
                .collect(Collectors.toList());
    }

    // ── Join / Leave ──────────────────────────────────────────────────────────

    @Transactional
    public List<VoiceParticipantResponse> joinRoom(Long roomId, User user) {
        VoiceRoom room = findActiveRoom(roomId);

        Integer max = room.getMaxParticipants();
        if (max != null) {
            long current = participantRepository.countByRoomAndLeftAtIsNull(room);
            if (current >= max) {
                throw new RuntimeException("Room is full (" + max + " participants max)");
            }
        }

        // Create or re-activate participant entry
        participantRepository.findByRoomAndUserAndLeftAtIsNull(room, user).ifPresent(p -> {
            // User already in room — no-op
        });
        if (participantRepository.findByRoomAndUserAndLeftAtIsNull(room, user).isEmpty()) {
            VoiceRoomParticipant p = VoiceRoomParticipant.builder()
                    .room(room)
                    .user(user)
                    .build();
            participantRepository.save(p);
        }

        List<VoiceParticipantResponse> participants = participantRepository
                .findByRoomAndLeftAtIsNull(room).stream()
                .map(VoiceParticipantResponse::from)
                .collect(Collectors.toList());

        // Broadcast JOIN event
        RoomEventMessage event = new RoomEventMessage();
        event.setType("JOIN");
        event.setRoomId(roomId);
        event.setUserId(user.getId());
        event.setUserNickname(user.getNickname());
        event.setUserFullName(user.getFullName());
        event.setUserAvatarUrl(user.getAvatarUrl());
        messagingTemplate.convertAndSend("/topic/voice/" + roomId + "/events", event);

        return participants;
    }

    @Transactional
    public void leaveRoom(Long roomId, User user) {
        VoiceRoom room = voiceRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        participantRepository.findByRoomAndUserAndLeftAtIsNull(room, user)
                .ifPresent(p -> {
                    p.setLeftAt(LocalDateTime.now());
                    participantRepository.save(p);
                });

        // Broadcast LEAVE event
        RoomEventMessage event = new RoomEventMessage();
        event.setType("LEAVE");
        event.setRoomId(roomId);
        event.setUserId(user.getId());
        event.setUserNickname(user.getNickname());
        event.setUserFullName(user.getFullName());
        event.setUserAvatarUrl(user.getAvatarUrl());
        messagingTemplate.convertAndSend("/topic/voice/" + roomId + "/events", event);
    }

    // ── End ───────────────────────────────────────────────────────────────────

    @Transactional
    public void endRoom(Long roomId, User user) {
        VoiceRoom room = findActiveRoom(roomId);

        boolean isCreator = room.getCreator().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;
        if (!isCreator && !isAdmin) {
            throw new RuntimeException("Only the room creator or an admin can end this room");
        }

        participantRepository.leaveAllInRoom(room);

        room.setStatus(VoiceRoomStatus.ENDED);
        room.setEndedAt(LocalDateTime.now());
        voiceRoomRepository.save(room);

        RoomEventMessage event = new RoomEventMessage();
        event.setType("ROOM_ENDED");
        event.setRoomId(roomId);
        messagingTemplate.convertAndSend("/topic/voice/" + roomId + "/events", event);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private VoiceRoom findActiveRoom(Long roomId) {
        VoiceRoom room = voiceRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        if (room.getStatus() != VoiceRoomStatus.ACTIVE) {
            throw new RuntimeException("Room is no longer active");
        }
        return room;
    }
}
