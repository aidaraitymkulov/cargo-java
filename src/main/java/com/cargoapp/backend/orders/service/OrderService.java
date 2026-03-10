package com.cargoapp.backend.orders.service;

import com.cargoapp.backend.common.dto.PagedResponse;
import com.cargoapp.backend.common.exception.AppException;
import com.cargoapp.backend.orders.dto.OrderAdminResponse;
import com.cargoapp.backend.orders.dto.OrderDetailResponse;
import com.cargoapp.backend.orders.dto.OrderResponse;
import com.cargoapp.backend.orders.entity.OrderEntity;
import com.cargoapp.backend.orders.entity.OrderStatus;
import com.cargoapp.backend.orders.mapper.OrderMapper;
import com.cargoapp.backend.orders.repository.OrderRepository;
import com.cargoapp.backend.products.mapper.ProductMapper;
import com.cargoapp.backend.products.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public PagedResponse<OrderResponse> getMyOrders(UUID userId, String status, int page, int pageSize) {
        var pageable = PageRequest.of(page - 1, pageSize, Sort.by("createdAt").descending());
        Page<OrderEntity> result = status != null
                ? orderRepository.findByUser_IdAndStatus(userId, OrderStatus.valueOf(status), pageable)
                : orderRepository.findByUser_Id(userId, pageable);

        var items = result.getContent().stream()
                .map(orderMapper::toOrderResponse)
                .toList();

        return new PagedResponse<>(items, page, pageSize, result.getTotalElements());
    }

    public OrderDetailResponse getOrderById(UUID userId, UUID orderId) {
        OrderEntity order = orderRepository.findByIdAndUser_Id(orderId, userId)
                .orElseThrow(() -> new AppException("ORDER_NOT_FOUND", HttpStatus.NOT_FOUND, "Заказ не найден"));

        var products = productRepository.findByOrderId(orderId).stream()
                .map(productMapper::toProductResponse)
                .toList();

        return new OrderDetailResponse(orderMapper.toOrderResponse(order), products);
    }

    public PagedResponse<OrderAdminResponse> getUserOrdersForAdmin(UUID userId, int page, int pageSize) {
        var pageable = PageRequest.of(page - 1, pageSize, Sort.by("createdAt").descending());
        Page<OrderEntity> result = orderRepository.findByUser_Id(userId, pageable);

        var items = result.getContent().stream()
                .map(orderMapper::toOrderAdminResponse)
                .toList();

        return new PagedResponse<>(items, page, pageSize, result.getTotalElements());
    }
}
