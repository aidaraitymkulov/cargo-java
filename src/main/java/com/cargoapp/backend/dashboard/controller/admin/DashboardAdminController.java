package com.cargoapp.backend.dashboard.controller.admin;

import com.cargoapp.backend.common.annotation.CurrentUserId;
import com.cargoapp.backend.dashboard.dto.DailyStatResponse;
import com.cargoapp.backend.dashboard.dto.DashboardSummaryResponse;
import com.cargoapp.backend.dashboard.service.DashboardService;
import com.cargoapp.backend.products.entity.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/dashboard")
@PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
@RequiredArgsConstructor
public class DashboardAdminController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary(
            @CurrentUserId UUID currentUserId,
            @RequestParam(required = false) UUID branchId,
            Authentication authentication) {
        boolean isSuperAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        return dashboardService.getSummary(currentUserId, branchId, isSuperAdmin);
    }

    @GetMapping("/charts/users")
    public List<DailyStatResponse> getUsersChart(
            @CurrentUserId UUID currentUserId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID branchId) {
        return dashboardService.getUsersChart(currentUserId, branchId, from, to);
    }

    @GetMapping("/charts/products/delivered")
    public List<DailyStatResponse> getDeliveredChart(
            @CurrentUserId UUID currentUserId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID branchId) {
        return dashboardService.getProductsChart(currentUserId, branchId, ProductStatus.DELIVERED, from, to);
    }
}
