package com.englishplatform.dto.request;

public class UpdateUserRequest {
    private String fullName;
    private String nickname;
    private String email;
    private String password;

    public String getFullName() { return fullName; }
    public void setFullName(String v) { this.fullName = v; }
    public String getNickname() { return nickname; }
    public void setNickname(String v) { this.nickname = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getPassword() { return password; }
    public void setPassword(String v) { this.password = v; }
}
