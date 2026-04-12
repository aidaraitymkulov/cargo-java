package com.cargoapp.backend.managers.controller.admin;

import com.cargoapp.backend.managers.dto.CreateManagerRequest;
import com.cargoapp.backend.managers.dto.ManagerResponse;
import com.cargoapp.backend.managers.dto.UpdateManagerRequest;
import com.cargoapp.backend.managers.service.ManagerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/managers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class ManagerAdminController {

    private final ManagerService managerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ManagerResponse createManager(@Valid @RequestBody CreateManagerRequest request) {
        return managerService.createManager(request);
    }

    @GetMapping
    public List<ManagerResponse> getManagers() {
        return managerService.getManagers();
    }

    @GetMapping("/{managerId}")
    public ManagerResponse getManagerById(@PathVariable UUID managerId) {
        return managerService.getManagerById(managerId);
    }

    @PatchMapping("/{managerId}")
    public ManagerResponse updateManager(
            @PathVariable UUID managerId,
            @Valid @RequestBody UpdateManagerRequest request
    ) {
        return managerService.updateManager(managerId, request);
    }

    @DeleteMapping("/{managerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteManager(@PathVariable UUID managerId, Authentication authentication) {
        UUID currentManagerId = UUID.fromString(authentication.getName());
        managerService.deleteManager(managerId, currentManagerId);
    }
}
