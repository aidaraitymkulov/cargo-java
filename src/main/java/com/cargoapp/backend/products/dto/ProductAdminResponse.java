package com.cargoapp.backend.products.dto;

import com.cargoapp.backend.products.entity.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductAdminResponse(
        UUID id,
        String hatch,
        UUID userId,
        String firstName,
        String lastName,
        String personalCode,
        ProductStatus status,
        BigDecimal price,
        BigDecimal weight,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
