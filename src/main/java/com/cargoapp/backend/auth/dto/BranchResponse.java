package com.cargoapp.backend.auth.dto;

import java.util.UUID;

public record BranchResponse(
        UUID id,
        String address,
        String personalCodePrefix,
        boolean isActive
) {}
