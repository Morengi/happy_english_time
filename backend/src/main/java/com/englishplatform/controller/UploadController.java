package com.englishplatform.controller;

import com.englishplatform.entity.Role;
import com.englishplatform.entity.User;
import com.englishplatform.service.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private final FileService fileService;

    public UploadController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * Upload an inline image for the rich text editor.
     * Returns the public URL of the uploaded image.
     */
    @PostMapping("/image")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) throws IOException {

        if (user.getRole() == Role.STUDENT) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        FileService.StoredFile stored = fileService.storeFile(file);
        return ResponseEntity.ok(Map.of("url", "/uploads/lessons/" + stored.storedName()));
    }
}
