package com.cargoapp.backend.users.controller.admin;

import com.cargoapp.backend.common.annotation.CurrentUserId;
import com.cargoapp.backend.common.dto.PagedResponse;
import com.cargoapp.backend.users.dto.UserResponse;
import com.cargoapp.backend.users.dto.UserStatsResponse;
import com.cargoapp.backend.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
public class UserAdminController {

    private final UserService userService;

    @GetMapping
    public PagedResponse<UserResponse> getUsers(
            @CurrentUserId UUID currentUserId,
            @RequestParam(required = false) String prefix,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return userService.getUsers(currentUserId, prefix, code, branchId, role, page, pageSize);
    }

    @GetMapping("/stats")
    public UserStatsResponse getUserStats(
            @CurrentUserId UUID currentUserId,
            @RequestParam(required = false) UUID branchId
    ) {
        return userService.getUserStats(currentUserId, branchId);
    }

    @GetMapping("/{userId}")
    public UserResponse getUserById(@PathVariable UUID userId) {
        return userService.getUserById(userId);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
    }

    @PatchMapping("/{userId}/chat-ban")
    public UserResponse chatBan(@PathVariable UUID userId) {
        return userService.chatBan(userId);
    }

    @PatchMapping("/{userId}/chat-unban")
    public UserResponse chatUnban(@PathVariable UUID userId) {
        return userService.chatUnban(userId);
    }

}
