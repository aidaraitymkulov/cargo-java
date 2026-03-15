package com.cargoapp.backend.orders.repository;

import com.cargoapp.backend.orders.entity.OrderEntity;
import com.cargoapp.backend.orders.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    Page<OrderEntity> findByUser_Id(UUID userId, Pageable pageable);

    Page<OrderEntity> findByUser_IdAndStatus(UUID userId, OrderStatus status, Pageable pageable);

    Optional<OrderEntity> findByIdAndUser_Id(UUID id, UUID userId);

    @Query("SELECT o FROM OrderEntity o JOIN FETCH o.user JOIN FETCH o.branch WHERE o.user.id = :userId")
    Page<OrderEntity> findByUserIdWithDetails(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Поиск заказов для delivered-импорта: все PENDING_PICKUP заказы клиента в конкретном филиале.
     */
    List<OrderEntity> findByUser_IdAndBranch_IdAndStatus(UUID userId, UUID branchId, OrderStatus status);

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.branch.id = :branchId AND o.status = 'PENDING_PICKUP'")
    long countPendingPickupByBranch(@Param("branchId") UUID branchId);

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.status = 'PENDING_PICKUP'")
    long countPendingPickup();

    @Query("SELECT SUM(o.price), COUNT(o) FROM OrderEntity o WHERE o.branch.id = :branchId AND o.status = 'DELIVERED' AND o.updatedAt >= :from AND o.updatedAt < :to")
    Object[] getRevenueByBranch(@Param("branchId") UUID branchId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT SUM(o.price), COUNT(o) FROM OrderEntity o WHERE o.status = 'DELIVERED' AND o.updatedAt >= :from AND o.updatedAt < :to")
    Object[] getRevenue(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
