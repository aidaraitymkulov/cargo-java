package com.cargoapp.backend.auth.dto;

public record WebAuthResponse(
        boolean success,
        UserResponse user
) {}
