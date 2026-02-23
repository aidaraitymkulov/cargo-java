package com.cargoapp.backend.users.repository;

import com.cargoapp.backend.users.entity.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UUID> {
    Optional<UserRoleEntity> findByRoleName(String roleName);
}
