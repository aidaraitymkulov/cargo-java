package com.cargoapp.backend.users.repository;

import com.cargoapp.backend.users.entity.UserPersonalCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserPersonalCodeRepository extends JpaRepository<UserPersonalCodeEntity, UUID> {
    Optional<UserPersonalCodeEntity> findByPersonalCodeAndActiveTrue(String personalCode);
    List<UserPersonalCodeEntity> findAllByUser_Id(UUID userId);
}
