package com.cargoapp.backend.auth.repository;

import com.cargoapp.backend.auth.entity.PasswordResetEntity;
import com.cargoapp.backend.auth.entity.PasswordResetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetRepository extends JpaRepository<PasswordResetEntity, UUID> {
    Optional<PasswordResetEntity> findByUser_IdAndStatus(UUID userId, PasswordResetStatus status);
    Optional<PasswordResetEntity> findByResetTokenAndStatus(UUID resetToken, PasswordResetStatus status);

    @Modifying
    @Query("UPDATE PasswordResetEntity r SET r.status = 'USED' WHERE r.user.id = :userId AND r.status IN ('PENDING', 'VERIFIED')")
    void invalidateActiveByUserId(@Param("userId") UUID userId);
}
