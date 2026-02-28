package com.cargoapp.backend.branches.repository;

import com.cargoapp.backend.branches.entity.BranchEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BranchRepository extends JpaRepository<BranchEntity, UUID> {
    Optional<BranchEntity> findByPersonalCodePrefix(String prefix);
    boolean existsByPersonalCodePrefix(String prefix);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BranchEntity b WHERE b.id = :id")
    Optional<BranchEntity> findByIdForUpdate(@Param("id") UUID id);
}
