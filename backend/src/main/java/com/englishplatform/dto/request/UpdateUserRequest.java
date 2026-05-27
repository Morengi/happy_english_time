package com.englishplatform.dto.request;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String fullName;
    private String nickname;
    private String email;
    private String password;
}
