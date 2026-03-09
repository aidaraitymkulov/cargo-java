package com.cargoapp.backend.products.dto;

import com.cargoapp.backend.products.entity.ProductStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProductHistoryEntry(
        UUID id,
        ProductStatus status,
        LocalDateTime createdAt
) {}
