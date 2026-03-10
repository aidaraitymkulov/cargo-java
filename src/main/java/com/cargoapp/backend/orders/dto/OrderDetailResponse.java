package com.cargoapp.backend.orders.dto;

import com.cargoapp.backend.orders.entity.OrderStatus;
import com.cargoapp.backend.products.dto.ProductResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderDetailResponse(
        UUID id,
        BigDecimal price,
        BigDecimal weight,
        int itemCount,
        OrderStatus status,
        List<ProductResponse> products,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
