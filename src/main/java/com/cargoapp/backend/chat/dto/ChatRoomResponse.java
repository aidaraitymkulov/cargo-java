package com.cargoapp.backend.chat.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatRoomResponse(
        UUID id,
        UUID userId,
        String userFullName,
        String userPersonalCode,
        UUID branchId,
        String branchAddress,
        long unreadCount,
        ChatMessageResponse lastMessage,
        LocalDateTime createdAt
) {}