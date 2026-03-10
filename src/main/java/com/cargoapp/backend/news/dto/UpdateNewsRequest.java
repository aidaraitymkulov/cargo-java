package com.cargoapp.backend.news.dto;

import jakarta.validation.constraints.Size;

public record UpdateNewsRequest(
        @Size(min = 1, message = "Заголовок не может быть пустым")
        String title,

        @Size(min = 1, message = "Контент не может быть пустым")
        String content
) {
}
