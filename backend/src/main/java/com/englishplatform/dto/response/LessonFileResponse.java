package com.englishplatform.dto.response;

import com.englishplatform.entity.LessonFile;

public class LessonFileResponse {
    private Long id;
    private String originalName;
    private String fileType;
    private Long fileSize;
    private boolean isImage;
    private boolean isVideo;
    private String url;
    private int sortOrder;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String v) { this.originalName = v; }
    public String getFileType() { return fileType; }
    public void setFileType(String v) { this.fileType = v; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long v) { this.fileSize = v; }
    public boolean isImage() { return isImage; }
    public void setImage(boolean v) { this.isImage = v; }
    public boolean isVideo() { return isVideo; }
    public void setVideo(boolean v) { this.isVideo = v; }
    public String getUrl() { return url; }
    public void setUrl(String v) { this.url = v; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int v) { this.sortOrder = v; }

    public static LessonFileResponse from(LessonFile file) {
        LessonFileResponse r = new LessonFileResponse();
        r.setId(file.getId());
        r.setOriginalName(file.getOriginalName());
        r.setFileType(file.getFileType());
        r.setFileSize(file.getFileSize());
        r.setImage(file.isImage());
        r.setVideo(file.isVideo());
        r.setUrl("/uploads/lessons/" + file.getStoredName());
        r.setSortOrder(file.getSortOrder());
        return r;
    }
}
