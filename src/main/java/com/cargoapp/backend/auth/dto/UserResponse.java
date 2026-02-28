package com.cargoapp.backend.auth.dto;

import com.cargoapp.backend.branches.dto.BranchResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phone,
        LocalDate dateOfBirth,
        String personalCode,
        String role,
        BranchResponse branch,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
