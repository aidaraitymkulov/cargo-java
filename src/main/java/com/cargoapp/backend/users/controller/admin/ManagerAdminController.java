package com.cargoapp.backend.users.controller.admin;

import com.cargoapp.backend.common.dto.PagedResponse;
import com.cargoapp.backend.users.dto.CreateManagerRequest;
import com.cargoapp.backend.users.dto.UpdateManagerRequest;
import com.cargoapp.backend.users.dto.UserResponse;
import com.cargoapp.backend.users.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/managers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class ManagerAdminController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createManager(@Valid @RequestBody CreateManagerRequest request) {
        return userService.createManager(request);
    }

    @GetMapping
    public PagedResponse<UserResponse> getManagers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return userService.getManagers(page, pageSize);
    }

    @PatchMapping("/{managerId}")
    public UserResponse updateManager(
            @PathVariable UUID managerId,
            @Valid @RequestBody UpdateManagerRequest request
    ) {
        return userService.updateManager(managerId, request);
    }

    @DeleteMapping("/{managerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteManager(@PathVariable UUID managerId, Authentication authentication) {
        UUID currentUserId = UUID.fromString(authentication.getName());
        userService.deleteManager(managerId, currentUserId);
    }
}
