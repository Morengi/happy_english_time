package com.englishplatform.dto.response;

import com.englishplatform.entity.Role;
import com.englishplatform.entity.VoiceRoomParticipant;

import java.time.LocalDateTime;

public class VoiceParticipantResponse {

    private Long userId;
    private String nickname;
    private String fullName;
    private String avatarUrl;
    private Role role;
    private LocalDateTime joinedAt;

    public VoiceParticipantResponse() {}

    public static VoiceParticipantResponse from(VoiceRoomParticipant p) {
        VoiceParticipantResponse r = new VoiceParticipantResponse();
        r.setUserId(p.getUser().getId());
        r.setNickname(p.getUser().getNickname());
        r.setFullName(p.getUser().getFullName());
        r.setAvatarUrl(p.getUser().getAvatarUrl());
        r.setRole(p.getUser().getRole());
        r.setJoinedAt(p.getJoinedAt());
        return r;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
}
