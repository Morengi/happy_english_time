package com.englishplatform.dto.request;

import jakarta.validation.constraints.NotBlank;

public class GroupRequest {
    @NotBlank
    private String name;

    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
}
