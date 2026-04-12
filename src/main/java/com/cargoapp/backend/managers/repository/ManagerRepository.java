package com.cargoapp.backend.managers.repository;

import com.cargoapp.backend.managers.entity.ManagerEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManagerRepository extends JpaRepository<ManagerEntity, UUID> {

    Optional<ManagerEntity> findByLogin(String login);

    boolean existsByLogin(String login);

    boolean existsByRole(String role);

    List<ManagerEntity> findAllByRoleNot(String role, Sort sort);
}
