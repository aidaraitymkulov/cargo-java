package com.cargoapp.backend.news.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateNewsRequest(
        @NotBlank(message = "Заголовок обязателен")
        String title,

        @NotBlank(message = "Контент обязателен")
        String content
) {
}
