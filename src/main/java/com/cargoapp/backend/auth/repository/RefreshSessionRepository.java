package com.cargoapp.backend.auth.repository;

import com.cargoapp.backend.auth.entity.RefreshSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionRepository extends JpaRepository<RefreshSessionEntity, UUID> {
    Optional<RefreshSessionEntity> findByJtiAndRevokedAtIsNull(String jti);
    void deleteAllByUser_Id(UUID userId);

    @Modifying
    @Query("UPDATE RefreshSessionEntity r SET r.revokedAt = :now WHERE r.jti = :jti")
    void revokeByJti(@Param("jti") String jti, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE RefreshSessionEntity r SET r.revokedAt = :now WHERE r.user.id = :userId AND r.revokedAt IS NULL")
    void revokeAllActiveByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
}
