package com.cargoapp.backend.chat.service;

import com.cargoapp.backend.chat.dto.ChatMessageResponse;
import com.cargoapp.backend.chat.dto.ChatRoomResponse;
import com.cargoapp.backend.chat.entity.ChatRoomEntity;
import com.cargoapp.backend.chat.entity.SenderType;
import com.cargoapp.backend.chat.mapper.ChatMapper;
import com.cargoapp.backend.chat.repository.ChatMessageRepository;
import com.cargoapp.backend.chat.repository.ChatRoomRepository;
import com.cargoapp.backend.common.constants.ManagerRole;
import com.cargoapp.backend.common.exception.AppException;
import com.cargoapp.backend.managers.entity.ManagerEntity;
import com.cargoapp.backend.managers.repository.ManagerRepository;
import com.cargoapp.backend.users.entity.UserEntity;
import com.cargoapp.backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ManagerRepository managerRepository;
    private final ChatMapper chatMapper;

    @Transactional
    public ChatRoomEntity getOrCreateRoom(UUID userId, UUID branchId) {
        return chatRoomRepository.findByUserIdAndBranchId(userId, branchId)
                .orElseGet(() -> {
                    UserEntity user = userRepository.findById(userId)
                            .orElseThrow(() -> new AppException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "Пользователь не найден"));

                    if (user.getBranch() == null || !user.getBranch().getId().equals(branchId)) {
                        throw new AppException("FORBIDDEN", HttpStatus.FORBIDDEN, "Нельзя создать чат для чужого филиала");
                    }

                    var room = new ChatRoomEntity();
                    room.setUser(user);
                    room.setBranch(user.getBranch());
                    return chatRoomRepository.save(room);
                });
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getRoomsForUser(UUID userId) {
        return chatRoomRepository.findByUserId(userId).stream()
                .map(room -> buildRoomResponse(room, SenderType.MANAGER))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getRoomsForManager(UUID managerId) {
        ManagerEntity manager = findManager(managerId);
        List<ChatRoomEntity> rooms;

        if (ManagerRole.SUPER_ADMIN.name().equals(manager.getRole())) {
            rooms = chatRoomRepository.findAll();
        } else {
            if (manager.getBranch() == null) {
                throw new AppException("MANAGER_NO_BRANCH", HttpStatus.BAD_REQUEST, "Менеджер не привязан к филиалу");
            }
            rooms = chatRoomRepository.findByBranchId(manager.getBranch().getId());
        }

        return rooms.stream()
                .map(room -> buildRoomResponse(room, SenderType.USER))
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatRoomEntity getRoomById(UUID roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new AppException("CHAT_ROOM_NOT_FOUND", HttpStatus.NOT_FOUND, "Чат не найден"));
    }

    @Transactional(readOnly = true)
    public void validateUserAccess(ChatRoomEntity room, UUID userId) {
        if (!room.getUser().getId().equals(userId)) {
            throw new AppException("FORBIDDEN", HttpStatus.FORBIDDEN, "Нет доступа к этому чату");
        }
    }

    @Transactional(readOnly = true)
    public void validateManagerAccess(ChatRoomEntity room, UUID managerId) {
        ManagerEntity manager = findManager(managerId);
        if (!ManagerRole.SUPER_ADMIN.name().equals(manager.getRole())) {
            if (manager.getBranch() == null) {
                throw new AppException("MANAGER_NO_BRANCH", HttpStatus.BAD_REQUEST, "Менеджер не привязан к филиалу");
            }
            if (!room.getBranch().getId().equals(manager.getBranch().getId())) {
                throw new AppException("FORBIDDEN", HttpStatus.FORBIDDEN, "Нет доступа к этому чату");
            }
        }
    }

    private ChatRoomResponse buildRoomResponse(ChatRoomEntity room, SenderType unreadFrom) {
        long unreadCount = chatMessageRepository.countUnreadByRoomAndSenderType(room.getId(), unreadFrom);

        ChatMessageResponse lastMessage = chatMessageRepository
                .findTopByChatRoomIdOrderByCreatedAtDesc(room.getId())
                .map(msg -> chatMapper.toMessageResponse(msg))
                .orElse(null);

        return chatMapper.toRoomResponse(room, unreadCount, lastMessage);
    }

    private ManagerEntity findManager(UUID managerId) {
        return managerRepository.findById(managerId)
                .orElseThrow(() -> new AppException("MANAGER_NOT_FOUND", HttpStatus.NOT_FOUND, "Менеджер не найден"));
    }
}