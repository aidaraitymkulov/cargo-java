package com.cargoapp.backend.products.dto;

import com.cargoapp.backend.products.entity.ProductStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProductAdminResponse(
        UUID id,
        String hatch,
        UUID userId,
        UUID orderId,
        ProductStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
