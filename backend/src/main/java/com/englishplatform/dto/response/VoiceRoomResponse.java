package com.englishplatform.dto.response;

import com.englishplatform.entity.VoiceRoom;
import com.englishplatform.entity.VoiceRoomStatus;

import java.time.LocalDateTime;

public class VoiceRoomResponse {

    private Long id;
    private String name;
    private Long creatorId;
    private String creatorName;
    private String creatorAvatarUrl;
    private Integer maxParticipants;
    private VoiceRoomStatus status;
    private int participantCount;
    private LocalDateTime createdAt;

    public VoiceRoomResponse() {}

    public static VoiceRoomResponse from(VoiceRoom room, int participantCount) {
        VoiceRoomResponse r = new VoiceRoomResponse();
        r.setId(room.getId());
        r.setName(room.getName());
        r.setCreatorId(room.getCreator().getId());
        r.setCreatorName(room.getCreator().getFullName());
        r.setCreatorAvatarUrl(room.getCreator().getAvatarUrl());
        r.setMaxParticipants(room.getMaxParticipants());
        r.setStatus(room.getStatus());
        r.setParticipantCount(participantCount);
        r.setCreatedAt(room.getCreatedAt());
        return r;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }

    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

    public String getCreatorAvatarUrl() { return creatorAvatarUrl; }
    public void setCreatorAvatarUrl(String creatorAvatarUrl) { this.creatorAvatarUrl = creatorAvatarUrl; }

    public Integer getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }

    public VoiceRoomStatus getStatus() { return status; }
    public void setStatus(VoiceRoomStatus status) { this.status = status; }

    public int getParticipantCount() { return participantCount; }
    public void setParticipantCount(int participantCount) { this.participantCount = participantCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
