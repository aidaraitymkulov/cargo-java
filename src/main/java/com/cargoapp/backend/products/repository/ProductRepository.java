package com.cargoapp.backend.products.repository;

import com.cargoapp.backend.products.entity.ProductEntity;
import com.cargoapp.backend.products.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {
    Page<ProductEntity> findByUser_Id(UUID userId, Pageable pageable);

    Page<ProductEntity> findByUser_IdAndStatus(UUID userId, ProductStatus productStatus, Pageable pageable);

    Optional<ProductEntity> findByIdAndUser_Id(UUID id, UUID userId);

    @Query("SELECT p.status, COUNT(p) FROM ProductEntity p WHERE p.user.id = :userId GROUP BY p.status")
    List<Object[]> countByStatusForUser(@Param("userId") UUID userId);

    @Query("SELECT MAX(p.updatedAt) FROM ProductEntity p WHERE p.user.id = :userId")
    Optional<LocalDateTime> findLastUpdatedAtByUserId(@Param("userId") UUID userId);

    Optional<ProductEntity> findFirstByUser_PersonalCodeAndHatchAndStatusNotOrderByCreatedAtAsc(
            String personalCode,
            String hatch,
            ProductStatus excludedStatus
    );

    Optional<ProductEntity> findFirstByUser_IdAndHatchAndStatusNotOrderByCreatedAtAsc(
            UUID userId,
            String hatch,
            ProductStatus excludedStatus
    );

    Optional<ProductEntity> findFirstByUser_IdAndHatchAndStatusOrderByCreatedAtAsc(
            UUID userId,
            String hatch,
            ProductStatus status
    );

    @Query("SELECT COUNT(p) FROM ProductEntity p WHERE p.user.branch.id = :branchId AND p.status = 'ON_THE_WAY'")
    long countOnTheWayByBranch(@Param("branchId") UUID branchId);

    @Query("SELECT COUNT(p) FROM ProductEntity p WHERE p.status = 'ON_THE_WAY'")
    long countOnTheWay();

    @Query("SELECT COUNT(p) FROM ProductEntity p WHERE p.status = :status")
    long countByStatusAll(@Param("status") ProductStatus status);

    @Query("SELECT COUNT(p) FROM ProductEntity p WHERE p.status = :status AND p.user.branch.id = :branchId")
    long countByStatusAndBranch(@Param("status") ProductStatus status, @Param("branchId") UUID branchId);

    @Query("SELECT COALESCE(SUM(p.price), 0) FROM ProductEntity p WHERE p.status = :status AND p.updatedAt >= :from AND p.updatedAt < :to")
    BigDecimal revenueForPeriod(@Param("status") ProductStatus status, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(p.price), 0) FROM ProductEntity p WHERE p.status = :status AND p.updatedAt >= :from AND p.updatedAt < :to AND p.user.branch.id = :branchId")
    BigDecimal revenueForPeriodByBranch(@Param("status") ProductStatus status, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("branchId") UUID branchId);

    @Query("SELECT CAST(p.updatedAt AS date), COUNT(p) FROM ProductEntity p WHERE p.status = :status AND p.updatedAt >= :from AND p.updatedAt < :to GROUP BY CAST(p.updatedAt AS date) ORDER BY CAST(p.updatedAt AS date)")
    List<Object[]> countDailyByStatus(@Param("status") ProductStatus status, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT CAST(p.updatedAt AS date), COUNT(p) FROM ProductEntity p WHERE p.status = :status AND p.updatedAt >= :from AND p.updatedAt < :to AND p.user.branch.id = :branchId GROUP BY CAST(p.updatedAt AS date) ORDER BY CAST(p.updatedAt AS date)")
    List<Object[]> countDailyByStatusAndBranch(@Param("status") ProductStatus status, @Param("branchId") UUID branchId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Modifying
    @Query("UPDATE ProductEntity p SET p.status = :newStatus WHERE p.status = :oldStatus AND p.createdAt <= :before")
    int bulkUpdateStatus(@Param("oldStatus") ProductStatus oldStatus,
                         @Param("newStatus") ProductStatus newStatus,
                         @Param("before") LocalDateTime before);
}
