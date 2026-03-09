package com.cargoapp.backend.products.dto;

import java.util.List;

public record ProductHistoryResponse(
        List<ProductHistoryEntry> items,
        int total
) {}
