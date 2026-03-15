package com.cargoapp.backend.orders.dto;

import java.math.BigDecimal;

public record RevenueResponse(BigDecimal revenue, long ordersCount) {}
