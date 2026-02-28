package com.cargoapp.backend.users.controller.mobile;

import com.cargoapp.backend.users.dto.*;
import com.cargoapp.backend.users.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserMobileController {

    private final UserService userService;

    @GetMapping
    public UserResponse getMe(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return userService.getMe(userId);
    }

    @PatchMapping
    public UserResponse updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        return userService.updateProfile(userId, request);
    }

    @PatchMapping("/change-password")
    public Map<String, Boolean> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        userService.changePassword(userId, request);
        return Map.of("success", true);
    }

    @PatchMapping("/branch")
    public UserResponse changeBranch(
            Authentication authentication,
            @Valid @RequestBody ChangeBranchRequest request
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        return userService.changeBranch(userId, request);
    }

    @PostMapping("/deletion-request")
    @ResponseStatus(HttpStatus.CREATED)
    public DeletionResponseDto requestDeletion(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return userService.requestDeletion(userId);
    }

    @DeleteMapping("/deletion-request")
    public Map<String, Boolean> cancelDeletion(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        userService.cancelDeletion(userId);
        return Map.of("success", true);
    }
}
