package com.englishplatform.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lesson_files")
public class LessonFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "stored_name", nullable = false)
    private String storedName;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "is_image", nullable = false)
    private boolean isImage;

    @Column(name = "is_video", nullable = false)
    private boolean isVideo;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public LessonFile() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Lesson getLesson() { return lesson; }
    public void setLesson(Lesson lesson) { this.lesson = lesson; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String v) { this.originalName = v; }
    public String getStoredName() { return storedName; }
    public void setStoredName(String v) { this.storedName = v; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String v) { this.filePath = v; }
    public String getFileType() { return fileType; }
    public void setFileType(String v) { this.fileType = v; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long v) { this.fileSize = v; }
    public boolean isImage() { return isImage; }
    public void setImage(boolean v) { this.isImage = v; }
    public boolean isVideo() { return isVideo; }
    public void setVideo(boolean v) { this.isVideo = v; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int v) { this.sortOrder = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private Lesson lesson;
        private String originalName, storedName, filePath, fileType;
        private Long fileSize;
        private boolean isImage, isVideo;
        private int sortOrder;
        public Builder lesson(Lesson v) { this.lesson = v; return this; }
        public Builder originalName(String v) { this.originalName = v; return this; }
        public Builder storedName(String v) { this.storedName = v; return this; }
        public Builder filePath(String v) { this.filePath = v; return this; }
        public Builder fileType(String v) { this.fileType = v; return this; }
        public Builder fileSize(Long v) { this.fileSize = v; return this; }
        public Builder isImage(boolean v) { this.isImage = v; return this; }
        public Builder isVideo(boolean v) { this.isVideo = v; return this; }
        public Builder sortOrder(int v) { this.sortOrder = v; return this; }
        public LessonFile build() {
            LessonFile f = new LessonFile();
            f.lesson = lesson; f.originalName = originalName; f.storedName = storedName;
            f.filePath = filePath; f.fileType = fileType; f.fileSize = fileSize;
            f.isImage = isImage; f.isVideo = isVideo; f.sortOrder = sortOrder;
            return f;
        }
    }

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
