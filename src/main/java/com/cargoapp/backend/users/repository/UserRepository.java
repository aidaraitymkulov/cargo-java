package com.cargoapp.backend.users.repository;

import com.cargoapp.backend.users.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID>, JpaSpecificationExecutor<UserEntity> {

    Optional<UserEntity> findByLogin(String login);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByPersonalCode(String personalCode);

    boolean existsByLogin(String login);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.branch.id = :branchId")
    long countUsersByBranch(@Param("branchId") UUID branchId);

    @Query("SELECT COUNT(u) FROM UserEntity u")
    long countAllUsers();

    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.branch.id = :branchId AND u.createdAt >= :from AND u.createdAt < :to")
    long countNewUsersByBranch(@Param("branchId") UUID branchId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.createdAt >= :from AND u.createdAt < :to")
    long countNewUsers(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
