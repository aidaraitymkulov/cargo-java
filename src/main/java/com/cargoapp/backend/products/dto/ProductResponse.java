package com.cargoapp.backend.products.dto;

import com.cargoapp.backend.products.entity.ProductStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String hatch,
        ProductStatus status,
        UUID orderId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
