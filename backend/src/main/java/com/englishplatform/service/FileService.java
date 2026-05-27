package com.englishplatform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class FileService {

    @Value("${app.upload.lesson-dir}")
    private String lessonDir;

    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml"
    );

    private static final Set<String> VIDEO_TYPES = Set.of(
            "video/mp4", "video/webm", "video/ogg", "video/mpeg", "video/quicktime"
    );

    public record StoredFile(String storedName, String filePath, String fileType,
                              long fileSize, boolean isImage, boolean isVideo) {}

    public StoredFile storeFile(MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        String storedName = UUID.randomUUID() + extension;
        Path targetPath = Paths.get(lessonDir).resolve(storedName);
        Files.copy(file.getInputStream(), targetPath);

        String contentType = file.getContentType();
        boolean isImage = IMAGE_TYPES.contains(contentType);
        boolean isVideo = VIDEO_TYPES.contains(contentType);

        return new StoredFile(storedName, targetPath.toString(), contentType,
                file.getSize(), isImage, isVideo);
    }

    public void deleteFile(String storedName) {
        try {
            Path filePath = Paths.get(lessonDir).resolve(storedName);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Could not delete file: {}", storedName, e);
        }
    }
}
