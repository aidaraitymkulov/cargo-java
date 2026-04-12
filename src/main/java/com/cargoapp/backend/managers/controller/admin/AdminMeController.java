package com.cargoapp.backend.managers.controller.admin;

import com.cargoapp.backend.managers.dto.ManagerResponse;
import com.cargoapp.backend.managers.service.ManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminMeController {

    private final ManagerService managerService;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ManagerResponse getMe(Authentication authentication) {
        UUID currentManagerId = UUID.fromString(authentication.getName());
        return managerService.getManagerById(currentManagerId);
    }
}
