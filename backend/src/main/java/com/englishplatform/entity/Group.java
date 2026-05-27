package com.englishplatform.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "groups")
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "group_members",
        joinColumns = @JoinColumn(name = "group_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> members = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Group() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public User getTeacher() { return teacher; }
    public void setTeacher(User teacher) { this.teacher = teacher; }
    public Set<User> getMembers() { return members; }
    public void setMembers(Set<User> members) { this.members = members; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String name;
        private User teacher;
        public Builder name(String v) { this.name = v; return this; }
        public Builder teacher(User v) { this.teacher = v; return this; }
        public Group build() {
            Group g = new Group();
            g.name = name; g.teacher = teacher;
            return g;
        }
    }

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
