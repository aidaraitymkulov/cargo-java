package com.cargoapp.backend.chat.repository;

import com.cargoapp.backend.chat.entity.ChatRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatRoomRepository extends JpaRepository<ChatRoomEntity, UUID> {

    Optional<ChatRoomEntity> findByUserIdAndBranchId(UUID userId, UUID branchId);

    List<ChatRoomEntity> findByUserId(UUID userId);

    List<ChatRoomEntity> findByBranchId(UUID branchId);
}