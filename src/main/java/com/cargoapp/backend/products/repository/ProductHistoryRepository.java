package com.cargoapp.backend.products.repository;

import com.cargoapp.backend.products.entity.ProductHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductHistoryRepository extends JpaRepository<ProductHistoryEntity, UUID> {
    List<ProductHistoryEntity> findByProduct_IdOrderByCreatedAtAsc(UUID productId);
}
