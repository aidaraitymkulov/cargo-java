package com.cargoapp.backend.auth.repository;

import com.cargoapp.backend.auth.entity.RefreshSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionRepository extends JpaRepository<RefreshSessionEntity, UUID> {
    Optional<RefreshSessionEntity> findByJti(String jti);
    void deleteAllByUser_Id(UUID userId);
}
