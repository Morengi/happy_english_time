package com.englishplatform.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "voice_rooms")
public class VoiceRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VoiceRoomStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    public VoiceRoom() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = VoiceRoomStatus.ACTIVE;
        }
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public Long getId() { return id; }
    public String getName() { return name; }
    public User getCreator() { return creator; }
    public Integer getMaxParticipants() { return maxParticipants; }
    public VoiceRoomStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getEndedAt() { return endedAt; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setCreator(User creator) { this.creator = creator; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }
    public void setStatus(VoiceRoomStatus status) { this.status = status; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final VoiceRoom room = new VoiceRoom();

        public Builder name(String name) { room.name = name; return this; }
        public Builder creator(User creator) { room.creator = creator; return this; }
        public Builder maxParticipants(Integer maxParticipants) { room.maxParticipants = maxParticipants; return this; }
        public Builder status(VoiceRoomStatus status) { room.status = status; return this; }

        public VoiceRoom build() { return room; }
    }
}
