package com.englishplatform.dto.response;

import com.englishplatform.entity.Role;
import com.englishplatform.entity.User;

import java.time.LocalDateTime;

public class UserResponse {
    private Long id;
    private String fullName;
    private String nickname;
    private String email;
    private Role role;
    private String avatarUrl;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public String getFullName() { return fullName; }
    public void setFullName(String v) { this.fullName = v; }
    public String getNickname() { return nickname; }
    public void setNickname(String v) { this.nickname = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public Role getRole() { return role; }
    public void setRole(Role v) { this.role = v; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String v) { this.avatarUrl = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }

    public static UserResponse from(User user) {
        UserResponse r = new UserResponse();
        r.setId(user.getId());
        r.setFullName(user.getFullName());
        r.setNickname(user.getNickname());
        r.setEmail(user.getEmail());
        r.setRole(user.getRole());
        r.setAvatarUrl(user.getAvatarUrl());
        r.setCreatedAt(user.getCreatedAt());
        return r;
    }
}
