package com.cargoapp.backend.managers.repository;

import com.cargoapp.backend.managers.entity.ManagerRefreshSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ManagerRefreshSessionRepository extends JpaRepository<ManagerRefreshSessionEntity, UUID> {

    Optional<ManagerRefreshSessionEntity> findByJtiAndRevokedAtIsNull(String jti);

    @Modifying
    @Query("UPDATE ManagerRefreshSessionEntity r SET r.revokedAt = :now WHERE r.jti = :jti")
    void revokeByJti(@Param("jti") String jti, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE ManagerRefreshSessionEntity r SET r.revokedAt = :now WHERE r.manager.id = :managerId AND r.revokedAt IS NULL")
    void revokeAllActiveByManagerId(@Param("managerId") UUID managerId, @Param("now") LocalDateTime now);
}
