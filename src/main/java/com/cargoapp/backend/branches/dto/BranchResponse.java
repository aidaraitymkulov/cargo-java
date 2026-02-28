package com.cargoapp.backend.branches.dto;

import java.util.UUID;

public record BranchResponse(
        UUID id,
        String address,
        String personalCodePrefix,
        boolean isActive
) {
}
