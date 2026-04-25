package com.cargoapp.backend.managers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateManagerRequest(

        @NotBlank(message = "Логин обязателен")
        @Size(min = 3, message = "Логин минимум 3 символа")
        String login,

        @NotBlank(message = "Пароль обязателен")
        @Size(min = 6, message = "Пароль минимум 6 символов")
        String password,

        @NotBlank(message = "Имя обязательно")
        String firstName,

        @NotBlank(message = "Фамилия обязательна")
        String lastName,

        @NotBlank(message = "Телефон обязателен")
        String phone,

        @NotNull(message = "Филиал обязателен")
        UUID branchId
) {}
