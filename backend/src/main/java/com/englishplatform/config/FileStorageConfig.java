package com.englishplatform.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileStorageConfig {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.upload.lesson-dir}")
    private String lessonDir;

    @Value("${app.upload.avatar-dir}")
    private String avatarDir;

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(uploadDir));
        Files.createDirectories(Paths.get(lessonDir));
        Files.createDirectories(Paths.get(avatarDir));
    }
}
