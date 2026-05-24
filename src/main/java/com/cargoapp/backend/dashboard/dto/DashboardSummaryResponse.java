package com.cargoapp.backend.dashboard.dto;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        long totalUsers,
        long newUsersThisMonth,
        long productsInChina,
        long productsOnTheWay,
        long productsAwaitingPickup,
        BigDecimal revenueThisWeek
) {}
