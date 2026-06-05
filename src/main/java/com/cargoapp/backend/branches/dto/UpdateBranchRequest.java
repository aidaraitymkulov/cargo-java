package com.cargoapp.backend.branches.dto;

public record UpdateBranchRequest(
        String address,
        Double latitude,
        Double longitude
) {
}
