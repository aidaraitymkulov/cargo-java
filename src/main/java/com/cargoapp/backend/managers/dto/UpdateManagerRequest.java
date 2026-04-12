package com.cargoapp.backend.managers.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateManagerRequest(

        @Size(min = 3, message = "Логин минимум 3 символа")
        String login,

        @Size(min = 6, message = "Пароль минимум 6 символов")
        String password,

        @Size(max = 100)
        String firstName,

        @Size(max = 100)
        String lastName,

        @Size(max = 20)
        String phone,

        UUID branchId
) {}
