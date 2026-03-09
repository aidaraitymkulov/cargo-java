package com.cargoapp.backend.products.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ItemsSummaryResponse(
        Map<String, Long> productsByStatus,
        long activeOrdersCount,
        long deliveredOrdersCount,
        LocalDateTime lastUpdatedAt
) {}
