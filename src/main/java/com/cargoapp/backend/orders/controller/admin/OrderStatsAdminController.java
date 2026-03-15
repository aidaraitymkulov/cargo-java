package com.cargoapp.backend.orders.controller.admin;

import com.cargoapp.backend.common.annotation.CurrentUserId;
import com.cargoapp.backend.common.dto.CountResponse;
import com.cargoapp.backend.orders.dto.RevenueResponse;
import com.cargoapp.backend.orders.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
public class OrderStatsAdminController {

    private final OrderService orderService;

    @GetMapping("/stats")
    public CountResponse getOrderStats(
            @CurrentUserId UUID currentUserId,
            @RequestParam(required = false) UUID branchId
    ) {
        return orderService.getOrderStats(currentUserId, branchId);
    }

    @GetMapping("/revenue")
    public RevenueResponse getRevenue(
            @CurrentUserId UUID currentUserId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month
    ) {
        LocalDate now = LocalDate.now();
        int effectiveYear = year > 0 ? year : now.getYear();
        int effectiveMonth = month > 0 ? month : now.getMonthValue();
        return orderService.getRevenue(currentUserId, branchId, effectiveYear, effectiveMonth);
    }
}
