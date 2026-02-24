package com.cargoapp.backend.auth.dto;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phone,
        String dateOfBirth,
        String personalCode,
        String role,
        BranchResponse branch,
        int status,
        String createdAt,
        String updatedAt
) {}
