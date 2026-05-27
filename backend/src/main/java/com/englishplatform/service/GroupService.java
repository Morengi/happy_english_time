package com.englishplatform.service;

import com.englishplatform.dto.request.GroupRequest;
import com.englishplatform.dto.response.GroupResponse;
import com.englishplatform.entity.Group;
import com.englishplatform.entity.Role;
import com.englishplatform.entity.User;
import com.englishplatform.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserService userService;

    public Group getById(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found: " + id));
    }

    @Transactional
    public GroupResponse createGroup(GroupRequest req, User teacher) {
        Group group = Group.builder()
                .name(req.getName())
                .teacher(teacher)
                .build();
        return GroupResponse.from(groupRepository.save(group));
    }

    @Transactional
    public GroupResponse updateGroup(Long id, GroupRequest req, User currentUser) {
        Group group = getById(id);
        checkManageAccess(group, currentUser);
        group.setName(req.getName());
        return GroupResponse.from(groupRepository.save(group));
    }

    @Transactional
    public void deleteGroup(Long id, User currentUser) {
        Group group = getById(id);
        checkManageAccess(group, currentUser);
        groupRepository.delete(group);
    }

    @Transactional
    public GroupResponse addMember(Long groupId, String nickname, User currentUser) {
        Group group = getById(groupId);
        checkManageAccess(group, currentUser);
        User member = userService.getByNickname(nickname);
        group.getMembers().add(member);
        return GroupResponse.from(groupRepository.save(group));
    }

    @Transactional
    public GroupResponse removeMember(Long groupId, Long userId, User currentUser) {
        Group group = getById(groupId);
        checkManageAccess(group, currentUser);
        User member = userService.getById(userId);
        group.getMembers().remove(member);
        return GroupResponse.from(groupRepository.save(group));
    }

    public List<GroupResponse> getGroupsForUser(User user) {
        List<Group> groups;
        if (user.getRole() == Role.ADMIN) {
            groups = groupRepository.findAll();
        } else if (user.getRole() == Role.TEACHER) {
            groups = groupRepository.findByTeacher(user);
        } else {
            groups = groupRepository.findByMember(user);
        }
        return groups.stream().map(GroupResponse::from).collect(Collectors.toList());
    }

    public GroupResponse getGroupDetail(Long id, User currentUser) {
        Group group = getById(id);
        checkViewAccess(group, currentUser);
        return GroupResponse.from(group);
    }

    private void checkManageAccess(Group group, User user) {
        if (user.getRole() == Role.ADMIN) return;
        if (!group.getTeacher().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
    }

    private void checkViewAccess(Group group, User user) {
        if (user.getRole() == Role.ADMIN) return;
        if (group.getTeacher().getId().equals(user.getId())) return;
        boolean isMember = group.getMembers().stream().anyMatch(m -> m.getId().equals(user.getId()));
        if (!isMember) throw new RuntimeException("Access denied");
    }
}
