package com.englishplatform.service;

import com.englishplatform.dto.request.CreateUserRequest;
import com.englishplatform.dto.request.UpdateUserRequest;
import com.englishplatform.dto.response.UserResponse;
import com.englishplatform.entity.Role;
import com.englishplatform.entity.User;
import com.englishplatform.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
        return userRepository.findAllWithFilters(role, search, pageable)
                .map(UserResponse::from);
    }

    public List<UserResponse> searchStudents(String search) {
        return userRepository.searchStudents(search).stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    public UserResponse getProfile(Long userId) {
        return UserResponse.from(getById(userId));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
