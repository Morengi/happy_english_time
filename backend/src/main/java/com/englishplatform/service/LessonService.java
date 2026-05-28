package com.englishplatform.service;

import com.englishplatform.dto.request.LessonRequest;
import com.englishplatform.dto.response.LessonResponse;
import com.englishplatform.entity.*;
import com.englishplatform.repository.GroupRepository;
import com.englishplatform.repository.LessonRepository;
import com.englishplatform.repository.WordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LessonService {

    private final LessonRepository lessonRepository;
    private final GroupRepository groupRepository;
    private final WordRepository wordRepository;
    private final UserService userService;
    private final FileService fileService;

    public LessonService(LessonRepository lessonRepository, GroupRepository groupRepository,
                         WordRepository wordRepository, UserService userService, FileService fileService) {
        this.lessonRepository = lessonRepository;
        this.groupRepository = groupRepository;
        this.wordRepository = wordRepository;
        this.userService = userService;
        this.fileService = fileService;
    }

    public Lesson getById(Long id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lesson not found: " + id));
    }

    public LessonFile getFile(Long lessonId, Long fileId, User currentUser) {
        Lesson lesson = getById(lessonId);
        checkViewAccess(lesson, currentUser);
        return lesson.getFiles().stream()
                .filter(f -> f.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("File not found: " + fileId));
    }

    @Transactional
    public LessonResponse createLesson(LessonRequest req, User teacher) {
        Lesson lesson = Lesson.builder()
                .title(req.getTitle())
                .content(req.getContent())
                .teacher(teacher)
                .build();
        return LessonResponse.from(lessonRepository.save(lesson));
    }

    @Transactional
    public LessonResponse updateLesson(Long id, LessonRequest req, User currentUser) {
        Lesson lesson = getById(id);
        checkManageAccess(lesson, currentUser);
        lesson.setTitle(req.getTitle());
        lesson.setContent(req.getContent());
        return LessonResponse.from(lessonRepository.save(lesson));
    }

    @Transactional
    public void deleteLesson(Long id, User currentUser) {
        Lesson lesson = getById(id);
        checkManageAccess(lesson, currentUser);
        lesson.getFiles().forEach(f -> fileService.deleteFile(f.getStoredName()));
        lessonRepository.delete(lesson);
    }

    @Transactional
    public LessonResponse grantAccess(Long lessonId, Long groupId, User currentUser) {
        Lesson lesson = getById(lessonId);
        checkManageAccess(lesson, currentUser);
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));
        lesson.getAccessGroups().add(group);
        return LessonResponse.from(lessonRepository.save(lesson));
    }

    @Transactional
    public LessonResponse revokeAccess(Long lessonId, Long groupId, User currentUser) {
        Lesson lesson = getById(lessonId);
        checkManageAccess(lesson, currentUser);
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));
        lesson.getAccessGroups().remove(group);
        return LessonResponse.from(lessonRepository.save(lesson));
    }

    @Transactional
    public LessonResponse uploadFile(Long lessonId, MultipartFile file, User currentUser) throws IOException {
        Lesson lesson = getById(lessonId);
        checkManageAccess(lesson, currentUser);

        FileService.StoredFile stored = fileService.storeFile(file);
        int sortOrder = lesson.getFiles().size();

        LessonFile lessonFile = LessonFile.builder()
                .lesson(lesson)
                .originalName(file.getOriginalFilename())
                .storedName(stored.storedName())
                .filePath(stored.filePath())
                .fileType(stored.fileType())
                .fileSize(stored.fileSize())
                .isImage(stored.isImage())
                .isVideo(stored.isVideo())
                .sortOrder(sortOrder)
                .build();
        lesson.getFiles().add(lessonFile);
        return LessonResponse.from(lessonRepository.save(lesson));
    }

    @Transactional
    public void deleteFile(Long lessonId, Long fileId, User currentUser) {
        Lesson lesson = getById(lessonId);
        checkManageAccess(lesson, currentUser);
        lesson.getFiles().stream()
                .filter(f -> f.getId().equals(fileId))
                .findFirst()
                .ifPresent(f -> {
                    fileService.deleteFile(f.getStoredName());
                    lesson.getFiles().remove(f);
                });
        lessonRepository.save(lesson);
    }

    public List<LessonResponse> getLessonsForUser(User user) {
        List<Lesson> lessons;
        if (user.getRole() == Role.ADMIN) {
            lessons = lessonRepository.findAll();
        } else if (user.getRole() == Role.TEACHER) {
            lessons = lessonRepository.findByTeacher(user);
        } else {
            List<Group> groups = groupRepository.findByMember(user);
            lessons = lessonRepository.findAccessibleByGroups(groups);
        }
        return lessons.stream().map(LessonResponse::fromBasic).collect(Collectors.toList());
    }

    public LessonResponse getLessonDetail(Long id, User currentUser) {
        Lesson lesson = getById(id);
        checkViewAccess(lesson, currentUser);
        return LessonResponse.from(lesson);
    }

    private void checkManageAccess(Lesson lesson, User user) {
        if (user.getRole() == Role.ADMIN) return;
        if (!lesson.getTeacher().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
    }

    private void checkViewAccess(Lesson lesson, User user) {
        if (user.getRole() == Role.ADMIN) return;
        if (lesson.getTeacher().getId().equals(user.getId())) return;
        List<Group> userGroups = groupRepository.findByMember(user);
        boolean hasAccess = lesson.getAccessGroups().stream()
                .anyMatch(ag -> userGroups.stream().anyMatch(ug -> ug.getId().equals(ag.getId())));
        if (!hasAccess) throw new RuntimeException("Access denied");
    }
}
