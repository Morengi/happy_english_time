package com.englishplatform.service;

import com.englishplatform.dto.request.CreateUserRequest;
import com.englishplatform.dto.request.UpdateUserRequest;
import com.englishplatform.dto.response.UserResponse;
import com.englishplatform.entity.Role;
import com.englishplatform.entity.User;
import com.englishplatform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.upload.avatar-dir}")
    private String avatarDir;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String nickname) throws UsernameNotFoundException {
        return userRepository.findByNickname(nickname)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + nickname));
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    public User getByNickname(String nickname) {
        return userRepository.findByNickname(nickname)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + nickname));
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest req) {
        if (userRepository.existsByNickname(req.getNickname())) {
            throw new RuntimeException("Nickname already taken: " + req.getNickname());
        }
        User user = User.builder()
                .fullName(req.getFullName())
                .nickname(req.getNickname())
                .email(blankToNull(req.getEmail()))
                .password(passwordEncoder.encode(req.getPassword()))
                .role(req.getRole())
                .build();
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest req) {
        User user = getById(id);
        if (req.getFullName() != null) user.setFullName(req.getFullName());
        if (req.getNickname() != null && !req.getNickname().equals(user.getNickname())) {
            if (userRepository.existsByNickname(req.getNickname())) {
                throw new RuntimeException("Nickname already taken");
            }
            user.setNickname(req.getNickname());
        }
        if (req.getEmail() != null) user.setEmail(blankToNull(req.getEmail()));
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public Page<UserResponse> getAllUsers(Role role, String search, Pageable pageable) {
        String roleStr = (role != null) ? role.name() : null;
        return userRepository.findAllWithFilters(roleStr, search, pageable)
                .map(UserResponse::from);
    }

    public List<UserResponse> searchStudents(String search) {
        return userRepository.searchStudents(search).stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    public List<UserResponse> searchAllUsers(String search) {
        return userRepository.searchAllUsers(search).stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    public UserResponse getProfile(Long userId) {
        return UserResponse.from(getById(userId));
    }

    @Transactional
    public UserResponse uploadAvatar(Long userId, MultipartFile file) throws IOException {
        User user = getById(userId);

        // Delete old avatar file if it exists
        if (user.getAvatarUrl() != null) {
            String oldName = Paths.get(user.getAvatarUrl()).getFileName().toString();
            Path oldPath = Paths.get(avatarDir).resolve(oldName);
            Files.deleteIfExists(oldPath);
        }

        // Store new avatar with UUID name to avoid conflicts
        String ext = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
        }
        String storedName = UUID.randomUUID() + ext;
        // Use toAbsolutePath() so the path is resolved from the working directory,
        // not from Tomcat's temp dir (which is what transferTo() would do with a relative path).
        Path dest = Paths.get(avatarDir).toAbsolutePath().resolve(storedName);
        Files.createDirectories(dest.getParent());
        // Files.copy() works correctly regardless of working directory
        try (var in = file.getInputStream()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }

        user.setAvatarUrl("/uploads/avatars/" + storedName);
        return UserResponse.from(userRepository.save(user));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
