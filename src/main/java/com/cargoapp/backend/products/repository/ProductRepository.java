package com.cargoapp.backend.products.repository;

import com.cargoapp.backend.products.entity.ProductEntity;
import com.cargoapp.backend.products.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {
    Page<ProductEntity> findByUser_Id(UUID userId, Pageable pageable);

    Page<ProductEntity> findByUser_IdAndStatus(UUID userId, ProductStatus productStatus, Pageable pageable);

    Optional<ProductEntity> findByIdAndUser_Id(UUID id, UUID userId);

    List<ProductEntity> findByOrderId(UUID orderId);

    @Query("SELECT p.status, COUNT(p) FROM ProductEntity p WHERE p.user.id = :userId GROUP BY p.status")
    List<Object[]> countByStatusForUser(@Param("userId") UUID userId);

    @Query("SELECT MAX(p.updatedAt) FROM ProductEntity p WHERE p.user.id = :userId")
    Optional<LocalDateTime> findLastUpdatedAtByUserId(@Param("userId") UUID userId);

}
