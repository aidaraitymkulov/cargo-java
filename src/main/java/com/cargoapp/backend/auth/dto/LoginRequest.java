package com.cargoapp.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest (
        @NotBlank(message = "Логин обязателен")
        String login,

        @NotBlank(message = "Пароль обязателен")
        String password
){}
