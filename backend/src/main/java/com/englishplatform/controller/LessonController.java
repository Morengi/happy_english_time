package com.englishplatform.controller;

import com.englishplatform.dto.request.LessonRequest;
import com.englishplatform.dto.request.WordRequest;
import com.englishplatform.dto.response.LessonResponse;
import com.englishplatform.entity.User;
import com.englishplatform.entity.WordSourceType;
import com.englishplatform.service.GroupService;
import com.englishplatform.service.LessonService;
import com.englishplatform.service.VocabularyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;
    private final VocabularyService vocabularyService;
    private final GroupService groupService;

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

    @PostMapping("/{id}/words")
    public ResponseEntity<Void> addWordToLesson(@PathVariable Long id,
                                                 @Valid @RequestBody WordRequest req,
                                                 @AuthenticationPrincipal User user) {
        req.setSourceType(WordSourceType.LESSON);
        req.setLessonId(id);

        // Add to teacher's vocabulary
        vocabularyService.addWord(req, user);

        // Auto-add to all members of groups that have access to this lesson
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
