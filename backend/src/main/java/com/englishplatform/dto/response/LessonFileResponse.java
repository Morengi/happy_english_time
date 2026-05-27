package com.englishplatform.dto.response;

import com.englishplatform.entity.LessonFile;
import lombok.Data;

@Data
public class LessonFileResponse {
    private Long id;
    private String originalName;
    private String fileType;
    private Long fileSize;
    private boolean isImage;
    private boolean isVideo;
    private String url;
    private int sortOrder;

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
