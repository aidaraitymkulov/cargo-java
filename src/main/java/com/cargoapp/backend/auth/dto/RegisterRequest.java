package com.cargoapp.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record RegisterRequest(

        @NotBlank(message = "Логин обязателен")
        @Size(min = 3, message = "Логин минимум 3 символа")
        String login,

        @NotBlank(message = "Имя обязательно")
        String firstName,

        @NotBlank(message = "Фамилия обязательна")
        String lastName,

        @NotBlank(message = "Email обязателен")
        @Email(message = "Некорректный email")
        String email,

        @NotBlank(message = "Телефон обязателен")
        String phone,

        @NotNull(message = "Дата рождения обязательна")
        LocalDate dateOfBirth,

        @NotBlank(message = "Пароль обязателен")
        @Size(min = 6, message = "Пароль минимум 6 символов")
        String password,

        @NotNull(message = "Филиал обязателен")
        UUID branchId
) {}
