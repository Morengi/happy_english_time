package com.englishplatform.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Message() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }
    public User getReceiver() { return receiver; }
    public void setReceiver(User receiver) { this.receiver = receiver; }
    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { this.isRead = read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private User sender, receiver;
        private Group group;
        private String content;
        private boolean isRead;
        public Builder sender(User v) { this.sender = v; return this; }
        public Builder receiver(User v) { this.receiver = v; return this; }
        public Builder group(Group v) { this.group = v; return this; }
        public Builder content(String v) { this.content = v; return this; }
        public Builder isRead(boolean v) { this.isRead = v; return this; }
        public Message build() {
            Message m = new Message();
            m.sender = sender; m.receiver = receiver; m.group = group;
            m.content = content; m.isRead = isRead;
            return m;
        }
    }

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
