package com.englishplatform.controller;

import com.englishplatform.dto.request.TestSubmitRequest;
import com.englishplatform.dto.response.TestSessionResponse;
import com.englishplatform.dto.response.WordResponse;
import com.englishplatform.entity.TestDirection;
import com.englishplatform.entity.User;
import com.englishplatform.entity.WordFilterType;
import com.englishplatform.service.TestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private final TestService testService;

    public TestController(TestService testService) {
        this.testService = testService;
    }

    @GetMapping("/words")
    public ResponseEntity<List<WordResponse>> getTestWords(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "10") int count,
            @RequestParam(defaultValue = "ALL") WordFilterType filterType,
            @RequestParam(required = false) Long filterLessonId,
            @RequestParam(required = false) Long filterGroupId,
            @RequestParam(defaultValue = "EN_TO_RU") TestDirection direction) {
        return ResponseEntity.ok(testService.getTestWords(user, count, filterType,
                filterLessonId, filterGroupId, direction));
    }

    @PostMapping("/submit")
    public ResponseEntity<TestSessionResponse> submitTest(@Valid @RequestBody TestSubmitRequest req,
                                                           @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(testService.submitTest(req, user));
    }

    @GetMapping("/history")
    public ResponseEntity<List<TestSessionResponse>> getHistory(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(testService.getUserHistory(user));
    }
}
