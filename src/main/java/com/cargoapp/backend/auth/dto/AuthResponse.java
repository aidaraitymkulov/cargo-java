package com.cargoapp.backend.auth.dto;

import com.cargoapp.backend.users.dto.UserResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserResponse user
) {}
