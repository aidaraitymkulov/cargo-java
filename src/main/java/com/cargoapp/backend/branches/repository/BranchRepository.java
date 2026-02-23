package com.cargoapp.backend.branches.repository;

import com.cargoapp.backend.branches.entity.BranchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BranchRepository extends JpaRepository<BranchEntity, UUID> {
    Optional<BranchEntity> findByPersonalCodePrefix(String prefix);
    boolean existsByPersonalCodePrefix(String prefix);
}
