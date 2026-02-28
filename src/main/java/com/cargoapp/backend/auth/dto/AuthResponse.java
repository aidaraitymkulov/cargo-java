package com.cargoapp.backend.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserResponse user
) {}
