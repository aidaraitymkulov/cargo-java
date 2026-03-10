package com.cargoapp.backend.orders.controller.admin;

import com.cargoapp.backend.common.dto.PagedResponse;
import com.cargoapp.backend.orders.dto.OrderAdminResponse;
import com.cargoapp.backend.orders.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/users/{userId}/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
public class OrderAdminController {

    private final OrderService orderService;

    @GetMapping
    public PagedResponse<OrderAdminResponse> getUserOrders(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return orderService.getUserOrdersForAdmin(userId, page, pageSize);
    }
}
