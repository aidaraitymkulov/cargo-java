package com.cargoapp.backend.chat.repository;

import com.cargoapp.backend.chat.entity.ChatMessageEntity;
import com.cargoapp.backend.chat.entity.SenderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, UUID> {

    Page<ChatMessageEntity> findByChatRoomId(UUID chatRoomId, Pageable pageable);

    Optional<ChatMessageEntity> findTopByChatRoomIdOrderByCreatedAtDesc(UUID chatRoomId);

    @Query("SELECT COUNT(m) FROM ChatMessageEntity m WHERE m.chatRoom.id = :roomId AND m.senderType = :senderType AND m.isRead = false")
    long countUnreadByRoomAndSenderType(@Param("roomId") UUID roomId, @Param("senderType") SenderType senderType);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChatMessageEntity m SET m.isRead = true WHERE m.chatRoom.id = :roomId AND m.senderType = :senderType AND m.isRead = false")
    void markAllAsReadByRoomAndSenderType(@Param("roomId") UUID roomId, @Param("senderType") SenderType senderType);

    @Query("SELECT COUNT(m) FROM ChatMessageEntity m WHERE m.chatRoom.user.id = :userId AND m.senderType = 'MANAGER' AND m.isRead = false")
    long countUnreadForUser(@Param("userId") UUID userId);

    @Query("SELECT COUNT(m) FROM ChatMessageEntity m WHERE m.chatRoom.branch.id = :branchId AND m.senderType = 'USER' AND m.isRead = false")
    long countUnreadForBranch(@Param("branchId") UUID branchId);

    @Query("SELECT COUNT(m) FROM ChatMessageEntity m WHERE m.senderType = 'USER' AND m.isRead = false")
    long countUnreadForAllBranches();
}