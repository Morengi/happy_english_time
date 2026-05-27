package com.englishplatform.dto.response;

import com.englishplatform.entity.Group;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class GroupResponse {
    private Long id;
    private String name;
    private UserResponse teacher;
    private List<UserResponse> members;
    private int memberCount;
    private LocalDateTime createdAt;

    public static GroupResponse from(Group group) {
        GroupResponse r = new GroupResponse();
        r.setId(group.getId());
        r.setName(group.getName());
        r.setTeacher(UserResponse.from(group.getTeacher()));
        r.setMembers(group.getMembers().stream().map(UserResponse::from).collect(Collectors.toList()));
        r.setMemberCount(group.getMembers().size());
        r.setCreatedAt(group.getCreatedAt());
        return r;
    }

    public static GroupResponse fromBasic(Group group) {
        GroupResponse r = new GroupResponse();
        r.setId(group.getId());
        r.setName(group.getName());
        r.setTeacher(UserResponse.from(group.getTeacher()));
        r.setMemberCount(group.getMembers().size());
        r.setCreatedAt(group.getCreatedAt());
        return r;
    }
}
