package com.englishplatform.service;

import com.englishplatform.dto.request.LoginRequest;
import com.englishplatform.dto.response.AuthResponse;
import com.englishplatform.entity.User;
import com.englishplatform.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getNickname(), req.getPassword())
        );
        User user = userService.getByNickname(req.getNickname());
        String token = jwtService.generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .nickname(user.getNickname())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }
}
