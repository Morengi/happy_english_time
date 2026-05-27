package com.englishplatform.controller;

import com.englishplatform.dto.request.WordRequest;
import com.englishplatform.dto.response.WordResponse;
import com.englishplatform.entity.User;
import com.englishplatform.service.VocabularyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vocabulary")
@RequiredArgsConstructor
public class VocabularyController {

    private final VocabularyService vocabularyService;

    @GetMapping
    public ResponseEntity<List<WordResponse>> getVocabulary(@AuthenticationPrincipal User user,
                                                             @RequestParam(required = false) String source) {
        return ResponseEntity.ok(vocabularyService.getVocabulary(user, source));
    }

    @PostMapping
    public ResponseEntity<WordResponse> addWord(@Valid @RequestBody WordRequest req,
                                                 @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(vocabularyService.addWord(req, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WordResponse> updateWord(@PathVariable Long id,
                                                    @Valid @RequestBody WordRequest req,
                                                    @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(vocabularyService.updateWord(id, req, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWord(@PathVariable Long id,
                                            @AuthenticationPrincipal User user) {
        vocabularyService.deleteWord(id, user);
        return ResponseEntity.noContent().build();
    }
}
