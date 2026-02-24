package com.cargoapp.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "Refresh токен обязателен")
        String refreshToken
) {}
