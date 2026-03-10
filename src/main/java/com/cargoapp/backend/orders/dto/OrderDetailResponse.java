package com.cargoapp.backend.orders.dto;

import com.cargoapp.backend.products.dto.ProductResponse;

import java.util.List;

public record OrderDetailResponse(
        OrderResponse order,
        List<ProductResponse> products
) {}
