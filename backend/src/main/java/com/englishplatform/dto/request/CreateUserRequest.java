package com.englishplatform.dto.request;

import com.englishplatform.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
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
}
