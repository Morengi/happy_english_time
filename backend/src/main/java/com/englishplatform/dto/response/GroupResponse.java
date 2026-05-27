package com.englishplatform.dto.response;

import com.englishplatform.entity.Group;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class GroupResponse {
    private Long id;
    private String name;
    private UserResponse teacher;
    private List<UserResponse> members;
    private int memberCount;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public UserResponse getTeacher() { return teacher; }
    public void setTeacher(UserResponse v) { this.teacher = v; }
    public List<UserResponse> getMembers() { return members; }
    public void setMembers(List<UserResponse> v) { this.members = v; }
    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int v) { this.memberCount = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }

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
