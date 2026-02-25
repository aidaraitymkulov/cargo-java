package com.cargoapp.backend.auth.repository;

import com.cargoapp.backend.auth.entity.ConfirmationEntity;
import com.cargoapp.backend.auth.entity.ConfirmationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConfirmationRepository extends JpaRepository<ConfirmationEntity, UUID> {
    Optional<ConfirmationEntity> findByUser_Id(UUID userId);
    Optional<ConfirmationEntity> findByUser_IdAndConfirmationStatus(UUID userId, ConfirmationStatus status);
    void deleteByUser_Id(UUID userId);
}
