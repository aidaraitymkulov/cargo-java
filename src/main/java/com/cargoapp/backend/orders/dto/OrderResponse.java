package com.cargoapp.backend.orders.dto;

import com.cargoapp.backend.orders.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        BigDecimal price,
        BigDecimal weight,
        int itemCount,
        OrderStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
