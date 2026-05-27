package com.englishplatform.dto.response;

import com.englishplatform.entity.Lesson;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class LessonResponse {
    private Long id;
    private String title;
    private String content;
    private UserResponse teacher;
    private List<GroupResponse> accessGroups;
    private List<LessonFileResponse> files;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
    public UserResponse getTeacher() { return teacher; }
    public void setTeacher(UserResponse v) { this.teacher = v; }
    public List<GroupResponse> getAccessGroups() { return accessGroups; }
    public void setAccessGroups(List<GroupResponse> v) { this.accessGroups = v; }
    public List<LessonFileResponse> getFiles() { return files; }
    public void setFiles(List<LessonFileResponse> v) { this.files = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }

    public static LessonResponse from(Lesson lesson) {
        LessonResponse r = new LessonResponse();
        r.setId(lesson.getId());
        r.setTitle(lesson.getTitle());
        r.setContent(lesson.getContent());
        r.setTeacher(UserResponse.from(lesson.getTeacher()));
        r.setAccessGroups(lesson.getAccessGroups().stream()
                .map(GroupResponse::fromBasic).collect(Collectors.toList()));
        r.setFiles(lesson.getFiles().stream()
                .map(LessonFileResponse::from).collect(Collectors.toList()));
        r.setCreatedAt(lesson.getCreatedAt());
        r.setUpdatedAt(lesson.getUpdatedAt());
        return r;
    }

    public static LessonResponse fromBasic(Lesson lesson) {
        LessonResponse r = new LessonResponse();
        r.setId(lesson.getId());
        r.setTitle(lesson.getTitle());
        r.setTeacher(UserResponse.from(lesson.getTeacher()));
        r.setCreatedAt(lesson.getCreatedAt());
        r.setUpdatedAt(lesson.getUpdatedAt());
        return r;
    }
}
