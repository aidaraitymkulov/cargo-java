package com.cargoapp.backend.products.repository;

import com.cargoapp.backend.products.entity.ProductHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ProductHistoryRepository extends JpaRepository<ProductHistoryEntity, UUID> {
    List<ProductHistoryEntity> findByProduct_IdOrderByCreatedAtAsc(UUID productId);

    @Query(value = """
            SELECT CAST(ph.created_at AS DATE) AS date, COUNT(*) AS count
            FROM product_histories ph
            WHERE ph.status = :status
            AND ph.created_at >= :from AND ph.created_at < :to
            GROUP BY CAST(ph.created_at AS DATE)
            ORDER BY CAST(ph.created_at AS DATE)
            """, nativeQuery = true)
    List<Object[]> countDailyByStatus(
            @Param("status") String status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query(value = """
            SELECT CAST(ph.created_at AS DATE) AS date, COUNT(*) AS count
            FROM product_histories ph
            JOIN products p ON p.id = ph.product_id
            JOIN users u ON u.id = p.user_id
            WHERE ph.status = :status
            AND u.branch_id = :branchId
            AND ph.created_at >= :from AND ph.created_at < :to
            GROUP BY CAST(ph.created_at AS DATE)
            ORDER BY CAST(ph.created_at AS DATE)
            """, nativeQuery = true)
    List<Object[]> countDailyByStatusAndBranch(
            @Param("status") String status,
            @Param("branchId") UUID branchId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
