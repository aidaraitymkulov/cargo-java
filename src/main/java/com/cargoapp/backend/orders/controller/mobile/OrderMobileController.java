package com.cargoapp.backend.orders.controller.mobile;

import com.cargoapp.backend.common.annotation.CurrentUserId;
import com.cargoapp.backend.common.dto.PagedResponse;
import com.cargoapp.backend.orders.dto.OrderDetailResponse;
import com.cargoapp.backend.orders.dto.OrderResponse;
import com.cargoapp.backend.orders.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderMobileController {

    private final OrderService orderService;

    @GetMapping
    public PagedResponse<OrderResponse> getMyOrders(
            @CurrentUserId UUID userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return orderService.getMyOrders(userId, status, page, pageSize);
    }

    @GetMapping("/{orderId}")
    public OrderDetailResponse getOrderById(
            @CurrentUserId UUID userId,
            @PathVariable UUID orderId
    ) {
        return orderService.getOrderById(userId, orderId);
    }
}
