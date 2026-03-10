package com.cargoapp.backend.orders.repository;

import com.cargoapp.backend.orders.entity.OrderEntity;
import com.cargoapp.backend.orders.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    Page<OrderEntity> findByUser_Id(UUID userId, Pageable pageable);

    Page<OrderEntity> findByUser_IdAndStatus(UUID userId, OrderStatus status, Pageable pageable);

    Optional<OrderEntity> findByIdAndUser_Id(UUID id, UUID userId);

    @Query("SELECT o FROM OrderEntity o JOIN FETCH o.user JOIN FETCH o.branch WHERE o.user.id = :userId")
    Page<OrderEntity> findByUserIdWithDetails(@Param("userId") UUID userId, Pageable pageable);
}
