package com.englishplatform.dto.response;

import com.englishplatform.entity.Lesson;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class LessonResponse {
    private Long id;
    private String title;
    private String content;
    private UserResponse teacher;
    private List<GroupResponse> accessGroups;
    private List<LessonFileResponse> files;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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
