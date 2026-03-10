package com.cargoapp.backend.orders.mapper;

import com.cargoapp.backend.branches.mapper.BranchMapper;
import com.cargoapp.backend.orders.dto.OrderAdminResponse;
import com.cargoapp.backend.orders.dto.OrderResponse;
import com.cargoapp.backend.orders.entity.OrderEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderMapper {

    private final BranchMapper branchMapper;

    public OrderResponse toOrderResponse(OrderEntity order) {
        return new OrderResponse(
                order.getId(),
                order.getPrice(),
                order.getWeight(),
                order.getItemCount(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    public OrderAdminResponse toOrderAdminResponse(OrderEntity order) {
        return new OrderAdminResponse(
                order.getId(),
                order.getUser().getId(),
                branchMapper.toBranchResponse(order.getBranch()),
                order.getPrice(),
                order.getWeight(),
                order.getItemCount(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
