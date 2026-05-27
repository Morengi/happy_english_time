package com.englishplatform.dto.request;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
    @NotBlank
    private String nickname;
    @NotBlank
    private String password;

    public String getNickname() { return nickname; }
    public void setNickname(String v) { this.nickname = v; }
    public String getPassword() { return password; }
    public void setPassword(String v) { this.password = v; }
}
