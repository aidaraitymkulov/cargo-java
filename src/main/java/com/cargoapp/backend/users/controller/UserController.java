package com.cargoapp.backend.users.controller;

import com.cargoapp.backend.auth.dto.UserResponse;
import com.cargoapp.backend.auth.mapper.AuthMapper;
import com.cargoapp.backend.common.exception.AppException;
import com.cargoapp.backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final AuthMapper authMapper;

    @GetMapping
    public UserResponse getMe(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return userRepository.findById(userId)
                .map(authMapper::toUserResponse)
                .orElseThrow(() -> new AppException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "Пользователь не найден"));
    }
}
