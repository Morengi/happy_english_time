package com.englishplatform.dto.request;

import jakarta.validation.constraints.NotBlank;

public class LessonRequest {
    @NotBlank
    private String title;
    private String content;

    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
}
