package com.englishplatform.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "lessons")
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "lesson_access",
        joinColumns = @JoinColumn(name = "lesson_id"),
        inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    private Set<Group> accessGroups = new HashSet<>();

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<LessonFile> files = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Lesson() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public User getTeacher() { return teacher; }
    public void setTeacher(User teacher) { this.teacher = teacher; }
    public Set<Group> getAccessGroups() { return accessGroups; }
    public void setAccessGroups(Set<Group> accessGroups) { this.accessGroups = accessGroups; }
    public List<LessonFile> getFiles() { return files; }
    public void setFiles(List<LessonFile> files) { this.files = files; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String title, content;
        private User teacher;
        public Builder title(String v) { this.title = v; return this; }
        public Builder content(String v) { this.content = v; return this; }
        public Builder teacher(User v) { this.teacher = v; return this; }
        public Lesson build() {
            Lesson l = new Lesson();
            l.title = title; l.content = content; l.teacher = teacher;
            return l;
        }
    }

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
