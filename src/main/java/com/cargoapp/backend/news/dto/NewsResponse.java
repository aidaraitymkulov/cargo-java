package com.cargoapp.backend.news.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record NewsResponse(
        UUID id,
        String cover,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
