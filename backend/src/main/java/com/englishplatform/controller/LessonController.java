package com.englishplatform.controller;

import com.englishplatform.dto.request.LessonRequest;
import com.englishplatform.dto.request.WordRequest;
import com.englishplatform.dto.response.LessonResponse;
import com.englishplatform.entity.LessonFile;
import com.englishplatform.entity.User;
import com.englishplatform.entity.WordSourceType;
import com.englishplatform.service.GroupService;
import com.englishplatform.service.LessonService;
import com.englishplatform.service.VocabularyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lessons")
public class LessonController {

    private final LessonService lessonService;
    private final VocabularyService vocabularyService;
    private final GroupService groupService;

    @Value("${app.upload.lesson-dir}")
    private String lessonDir;

    public LessonController(LessonService lessonService, VocabularyService vocabularyService,
                            GroupService groupService) {
        this.lessonService = lessonService;
        this.vocabularyService = vocabularyService;
        this.groupService = groupService;
    }

    @GetMapping
    public ResponseEntity<List<LessonResponse>> getLessons(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(lessonService.getLessonsForUser(user));
    }

    @PostMapping
    public ResponseEntity<LessonResponse> createLesson(@Valid @RequestBody LessonRequest req,
                                                        @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(lessonService.createLesson(req, user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LessonResponse> getLesson(@PathVariable Long id,
                                                     @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(lessonService.getLessonDetail(id, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LessonResponse> updateLesson(@PathVariable Long id,
                                                        @Valid @RequestBody LessonRequest req,
                                                        @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(lessonService.updateLesson(id, req, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLesson(@PathVariable Long id,
                                              @AuthenticationPrincipal User user) {
        lessonService.deleteLesson(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/access")
    public ResponseEntity<LessonResponse> grantAccess(@PathVariable Long id,
                                                       @RequestBody Map<String, Long> body,
                                                       @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(lessonService.grantAccess(id, body.get("groupId"), user));
    }

    @DeleteMapping("/{id}/access/{groupId}")
    public ResponseEntity<LessonResponse> revokeAccess(@PathVariable Long id,
                                                        @PathVariable Long groupId,
                                                        @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(lessonService.revokeAccess(id, groupId, user));
    }

    @PostMapping("/{id}/files")
    public ResponseEntity<LessonResponse> uploadFile(@PathVariable Long id,
                                                      @RequestParam("file") MultipartFile file,
                                                      @AuthenticationPrincipal User user) throws IOException {
        return ResponseEntity.ok(lessonService.uploadFile(id, file, user));
    }

    @DeleteMapping("/{id}/files/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long id,
                                            @PathVariable Long fileId,
                                            @AuthenticationPrincipal User user) {
        lessonService.deleteFile(id, fileId, user);
        return ResponseEntity.noContent().build();
    }

    /**
     * Stream a file with Content-Disposition: attachment so mobile browsers
     * save it to disk instead of trying to open it inline.
     */
    @GetMapping("/{id}/files/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id,
                                                  @PathVariable Long fileId,
                                                  @AuthenticationPrincipal User user) {
        LessonFile file = lessonService.getFile(id, fileId, user);
        Resource resource = new FileSystemResource(
                Paths.get(lessonDir).resolve(file.getStoredName()).toFile());

        String encoded = URLEncoder.encode(file.getOriginalName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        // Build Content-Disposition: use filename*= for full Unicode support.
        // The legacy filename= parameter must contain only ASCII chars — omit it
        // for names that contain non-ASCII (e.g. Cyrillic), because Tomcat rejects
        // headers with non-printable/non-ASCII bytes (RFC 7230).
        boolean asciiOnly = file.getOriginalName().chars().allMatch(c -> c < 128);
        String disposition = asciiOnly
                ? "attachment; filename=\"" + file.getOriginalName() + "\"; filename*=UTF-8''" + encoded
                : "attachment; filename*=UTF-8''" + encoded;

        // Always force application/octet-stream so mobile browsers (iOS Safari,
        // Android Chrome) NEVER try to render the file inline — they always
        // save it to disk.  The filename extension keeps the correct file
        // association once saved.
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(resource);
    }

    @PostMapping("/{id}/words")
    public ResponseEntity<Void> addWordToLesson(@PathVariable Long id,
                                                 @Valid @RequestBody WordRequest req,
                                                 @AuthenticationPrincipal User user) {
        req.setSourceType(WordSourceType.LESSON);
        req.setLessonId(id);
        vocabularyService.addWord(req, user);
        lessonService.getById(id).getAccessGroups().forEach(group ->
            group.getMembers().forEach(member -> {
                try {
                    vocabularyService.addWord(req, member);
                } catch (RuntimeException e) {
                    // Skip duplicates
                }
            })
        );
        return ResponseEntity.ok().build();
    }
}
