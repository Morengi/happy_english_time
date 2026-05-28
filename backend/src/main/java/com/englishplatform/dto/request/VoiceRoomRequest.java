package com.englishplatform.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class VoiceRoomRequest {

    @NotBlank(message = "Room name is required")
    @Size(max = 100, message = "Room name must be at most 100 characters")
    private String name;

    @Min(value = 2, message = "maxParticipants must be at least 2")
    private Integer maxParticipants;  // null = unlimited

    public VoiceRoomRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }
}
