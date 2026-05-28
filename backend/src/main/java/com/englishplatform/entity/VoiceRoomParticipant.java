package com.englishplatform.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "voice_room_participants",
    uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "user_id"})
)
public class VoiceRoomParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private VoiceRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    public VoiceRoomParticipant() {}

    @PrePersist
    protected void onCreate() {
        this.joinedAt = LocalDateTime.now();
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public Long getId() { return id; }
    public VoiceRoom getRoom() { return room; }
    public User getUser() { return user; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public LocalDateTime getLeftAt() { return leftAt; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setId(Long id) { this.id = id; }
    public void setRoom(VoiceRoom room) { this.room = room; }
    public void setUser(User user) { this.user = user; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
    public void setLeftAt(LocalDateTime leftAt) { this.leftAt = leftAt; }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final VoiceRoomParticipant p = new VoiceRoomParticipant();

        public Builder room(VoiceRoom room) { p.room = room; return this; }
        public Builder user(User user) { p.user = user; return this; }

        public VoiceRoomParticipant build() { return p; }
    }
}
