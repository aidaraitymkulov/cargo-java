package com.cargoapp.backend.users.controller.mobile;

import com.cargoapp.backend.common.annotation.CurrentUserId;
import com.cargoapp.backend.users.dto.*;
import com.cargoapp.backend.users.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserMobileController {

    private final UserService userService;

    @GetMapping
    public UserResponse getMe(@CurrentUserId UUID userId) {
        return userService.getMe(userId);
    }

    @PatchMapping
    public UserResponse updateProfile(
            @CurrentUserId UUID userId,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return userService.updateProfile(userId, request);
    }

    @PatchMapping("/change-password")
    public Map<String, Boolean> changePassword(
            @CurrentUserId UUID userId,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(userId, request);
        return Map.of("success", true);
    }

    @PatchMapping("/branch")
    public UserResponse changeBranch(
            @CurrentUserId UUID userId,
            @Valid @RequestBody ChangeBranchRequest request
    ) {
        return userService.changeBranch(userId, request);
    }

    @PostMapping("/deletion-request")
    @ResponseStatus(HttpStatus.CREATED)
    public DeletionResponseDto requestDeletion(@CurrentUserId UUID userId) {
        return userService.requestDeletion(userId);
    }

    @DeleteMapping("/deletion-request")
    public Map<String, Boolean> cancelDeletion(@CurrentUserId UUID userId) {
        userService.cancelDeletion(userId);
        return Map.of("success", true);
    }
}
