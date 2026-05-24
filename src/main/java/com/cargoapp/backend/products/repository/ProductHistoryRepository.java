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
            SELECT CAST(p.updated_at AS DATE) AS date, COUNT(*) AS count
            FROM products p
            WHERE p.status = :status
            AND p.updated_at >= :from AND p.updated_at < :to
            GROUP BY CAST(p.updated_at AS DATE)
            ORDER BY CAST(p.updated_at AS DATE)
            """, nativeQuery = true)
    List<Object[]> countDailyByStatus(
            @Param("status") String status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query(value = """
            SELECT CAST(p.updated_at AS DATE) AS date, COUNT(*) AS count
            FROM products p
            JOIN users u ON u.id = p.user_id
            WHERE p.status = :status
            AND u.branch_id = :branchId
            AND p.updated_at >= :from AND p.updated_at < :to
            GROUP BY CAST(p.updated_at AS DATE)
            ORDER BY CAST(p.updated_at AS DATE)
            """, nativeQuery = true)
    List<Object[]> countDailyByStatusAndBranch(
            @Param("status") String status,
            @Param("branchId") UUID branchId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
