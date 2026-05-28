package com.englishplatform.dto.response;

import com.englishplatform.entity.Role;

public class AuthResponse {
    private String token;
    private Long userId;
    private String nickname;
    private String fullName;
    private Role role;
    private String avatarUrl;

    public AuthResponse() {}

    public String getToken() { return token; }
    public void setToken(String v) { this.token = v; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getNickname() { return nickname; }
    public void setNickname(String v) { this.nickname = v; }
    public String getFullName() { return fullName; }
    public void setFullName(String v) { this.fullName = v; }
    public Role getRole() { return role; }
    public void setRole(Role v) { this.role = v; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String v) { this.avatarUrl = v; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String token; private Long userId;
        private String nickname, fullName, avatarUrl; private Role role;
        public Builder token(String v) { this.token = v; return this; }
        public Builder userId(Long v) { this.userId = v; return this; }
        public Builder nickname(String v) { this.nickname = v; return this; }
        public Builder fullName(String v) { this.fullName = v; return this; }
        public Builder role(Role v) { this.role = v; return this; }
        public Builder avatarUrl(String v) { this.avatarUrl = v; return this; }
        public AuthResponse build() {
            AuthResponse r = new AuthResponse();
            r.token = token; r.userId = userId; r.nickname = nickname;
            r.fullName = fullName; r.role = role; r.avatarUrl = avatarUrl;
            return r;
        }
    }
}
