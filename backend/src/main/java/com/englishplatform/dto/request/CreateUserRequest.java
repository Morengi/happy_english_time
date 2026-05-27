package com.englishplatform.dto.request;

import com.englishplatform.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateUserRequest {
    @NotBlank
    private String fullName;
    @NotBlank
    @Size(min = 3, max = 50)
    private String nickname;
    private String email;
    @NotBlank
    @Size(min = 6)
    private String password;
    @NotNull
    private Role role;

    public String getFullName() { return fullName; }
    public void setFullName(String v) { this.fullName = v; }
    public String getNickname() { return nickname; }
    public void setNickname(String v) { this.nickname = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getPassword() { return password; }
    public void setPassword(String v) { this.password = v; }
    public Role getRole() { return role; }
    public void setRole(Role v) { this.role = v; }
}
