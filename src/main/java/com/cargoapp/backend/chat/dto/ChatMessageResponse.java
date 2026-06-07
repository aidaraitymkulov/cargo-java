package com.cargoapp.backend.chat.dto;

import com.cargoapp.backend.chat.entity.SenderType;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        UUID roomId,
        SenderType senderType,
        UUID senderId,
        String senderName,
        String content,
        boolean isRead,
        LocalDateTime createdAt
) {}