package com.cargoapp.backend.managers.dto;

import com.cargoapp.backend.branches.dto.BranchResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public record ManagerResponse(
        UUID id,
        String login,
        String firstName,
        String lastName,
        String phone,
        String role,
        BranchResponse branch,
        LocalDateTime createdAt
) {}
