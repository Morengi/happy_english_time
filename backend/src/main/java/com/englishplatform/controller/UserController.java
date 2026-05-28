package com.englishplatform.controller;

import com.englishplatform.dto.request.UpdateUserRequest;
import com.englishplatform.dto.response.UserResponse;
import com.englishplatform.entity.User;
import com.englishplatform.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getProfile(user.getId()));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(@AuthenticationPrincipal User user,
                                                       @RequestBody UpdateUserRequest req) {
        return ResponseEntity.ok(userService.updateUser(user.getId(), req));
    }

    @PostMapping("/avatar")
    public ResponseEntity<UserResponse> uploadAvatar(@RequestParam("file") MultipartFile file,
                                                      @AuthenticationPrincipal User user) throws IOException {
        return ResponseEntity.ok(userService.uploadAvatar(user.getId(), file));
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> searchStudents(@RequestParam String q) {
        return ResponseEntity.ok(userService.searchStudents(q));
    }

    @GetMapping("/search/all")
    public ResponseEntity<List<UserResponse>> searchAllUsers(@RequestParam String q) {
        return ResponseEntity.ok(userService.searchAllUsers(q));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getProfile(id));
    }
}
