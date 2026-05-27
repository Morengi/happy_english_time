package com.englishplatform.controller;

import com.englishplatform.dto.request.GroupRequest;
import com.englishplatform.dto.request.WordRequest;
import com.englishplatform.dto.response.GroupResponse;
import com.englishplatform.dto.response.MessageResponse;
import com.englishplatform.dto.response.RankingResponse;
import com.englishplatform.dto.response.WordResponse;
import com.englishplatform.entity.User;
import com.englishplatform.entity.WordSourceType;
import com.englishplatform.service.GroupService;
import com.englishplatform.service.MessageService;
import com.englishplatform.service.TestService;
import com.englishplatform.service.VocabularyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;
    private final MessageService messageService;
    private final TestService testService;
    private final VocabularyService vocabularyService;

    public GroupController(GroupService groupService, MessageService messageService,
                           TestService testService, VocabularyService vocabularyService) {
        this.groupService = groupService;
        this.messageService = messageService;
        this.testService = testService;
        this.vocabularyService = vocabularyService;
    }

    @GetMapping
    public ResponseEntity<List<GroupResponse>> getMyGroups(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(groupService.getGroupsForUser(user));
    }

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(@Valid @RequestBody GroupRequest req,
                                                      @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(groupService.createGroup(req, user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupResponse> getGroup(@PathVariable Long id,
                                                   @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(groupService.getGroupDetail(id, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GroupResponse> updateGroup(@PathVariable Long id,
                                                      @Valid @RequestBody GroupRequest req,
                                                      @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(groupService.updateGroup(id, req, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id,
                                             @AuthenticationPrincipal User user) {
        groupService.deleteGroup(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<GroupResponse> addMember(@PathVariable Long id,
                                                    @RequestBody Map<String, String> body,
                                                    @AuthenticationPrincipal User user) {
        String nickname = body.get("nickname");
        return ResponseEntity.ok(groupService.addMember(id, nickname, user));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<GroupResponse> removeMember(@PathVariable Long id,
                                                       @PathVariable Long userId,
                                                       @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(groupService.removeMember(id, userId, user));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<MessageResponse>> getGroupMessages(@PathVariable Long id) {
        return ResponseEntity.ok(messageService.getGroupMessages(id));
    }

    @GetMapping("/{id}/ranking")
    public ResponseEntity<List<RankingResponse>> getRanking(@PathVariable Long id) {
        return ResponseEntity.ok(testService.getGroupRanking(id));
    }

    @PostMapping("/{id}/words")
    public ResponseEntity<Void> addWordToGroup(@PathVariable Long id,
                                                @Valid @RequestBody WordRequest req,
                                                @AuthenticationPrincipal User user) {
        req.setSourceType(WordSourceType.GROUP);
        req.setGroupId(id);
        vocabularyService.addWord(req, user);
        groupService.getById(id).getMembers().forEach(member -> {
            try {
                vocabularyService.addWord(req, member);
            } catch (RuntimeException e) {
                // Skip duplicates
            }
        });
        return ResponseEntity.ok().build();
    }
}
