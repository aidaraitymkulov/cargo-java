package com.cargoapp.backend.chat.service;

import com.cargoapp.backend.chat.dto.ChatMessageResponse;
import com.cargoapp.backend.chat.dto.UnreadCountResponse;
import com.cargoapp.backend.chat.entity.ChatMessageEntity;
import com.cargoapp.backend.chat.entity.ChatRoomEntity;
import com.cargoapp.backend.chat.entity.SenderType;
import com.cargoapp.backend.chat.mapper.ChatMapper;
import com.cargoapp.backend.chat.repository.ChatMessageRepository;
import com.cargoapp.backend.common.constants.ManagerRole;
import com.cargoapp.backend.common.dto.PagedResponse;
import com.cargoapp.backend.common.exception.AppException;
import com.cargoapp.backend.managers.entity.ManagerEntity;
import com.cargoapp.backend.managers.repository.ManagerRepository;
import com.cargoapp.backend.users.entity.UserEntity;
import com.cargoapp.backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomService chatRoomService;
    private final UserRepository userRepository;
    private final ManagerRepository managerRepository;
    private final ChatMapper chatMapper;

    @Transactional
    public ChatMessageResponse sendMessage(UUID senderId, SenderType senderType, UUID roomId, String content) {
        if (senderType == SenderType.USER) {
            UserEntity user = userRepository.findById(senderId)
                    .orElseThrow(() -> new AppException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "Пользователь не найден"));
            if (user.isChatBanned()) {
                throw new AppException("CHAT_BANNED", HttpStatus.FORBIDDEN, "Вы заблокированы в чате");
            }
        }

        ChatRoomEntity room = chatRoomService.getRoomById(roomId);

        if (senderType == SenderType.USER) {
            chatRoomService.validateUserAccess(room, senderId);
        } else {
            chatRoomService.validateManagerAccess(room, senderId);
        }

        var message = new ChatMessageEntity();
        message.setChatRoom(room);
        message.setSenderType(senderType);
        message.setSenderId(senderId);
        message.setContent(content);

        ChatMessageEntity saved = chatMessageRepository.save(message);
        return chatMapper.toMessageResponse(saved, resolveSenderName(senderType, senderId));
    }

    @Transactional(readOnly = true)
    public PagedResponse<ChatMessageResponse> getMessages(UUID roomId, int page, int pageSize) {
        var pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = chatMessageRepository.findByChatRoomId(roomId, pageable);

        var messages = result.getContent().stream()
                .map(msg -> chatMapper.toMessageResponse(msg, resolveSenderName(msg.getSenderType(), msg.getSenderId())))
                .toList();

        return new PagedResponse<>(messages, page, pageSize, result.getTotalElements());
    }

    @Transactional
    public void markAsRead(UUID roomId, SenderType readerType) {
        SenderType oppositeType = (readerType == SenderType.USER) ? SenderType.MANAGER : SenderType.USER;
        chatMessageRepository.markAllAsReadByRoomAndSenderType(roomId, oppositeType);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCountForUser(UUID userId) {
        long count = chatMessageRepository.countUnreadForUser(userId);
        return new UnreadCountResponse(count);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCountForManager(UUID managerId) {
        ManagerEntity manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new AppException("MANAGER_NOT_FOUND", HttpStatus.NOT_FOUND, "Менеджер не найден"));

        long count;
        if (ManagerRole.SUPER_ADMIN.name().equals(manager.getRole())) {
            count = chatMessageRepository.countUnreadForAllBranches();
        } else {
            count = chatMessageRepository.countUnreadForBranch(manager.getBranch().getId());
        }

        return new UnreadCountResponse(count);
    }

    private String resolveSenderName(SenderType senderType, UUID senderId) {
        if (senderType == SenderType.USER) {
            return userRepository.findById(senderId)
                    .map(u -> u.getFirstName() + " " + u.getLastName())
                    .orElse("Удалённый пользователь");
        } else {
            return managerRepository.findById(senderId)
                    .map(m -> m.getFirstName() + " " + m.getLastName())
                    .orElse("Менеджер");
        }
    }
}