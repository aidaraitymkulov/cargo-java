package com.cargoapp.backend.chat.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        UUID roomId,
        String senderType,
        UUID senderId,
        String senderName,
        String content,
        boolean isRead,
        LocalDateTime createdAt
) {}