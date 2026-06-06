package com.cargoapp.backend.chat.mapper;

import com.cargoapp.backend.chat.dto.ChatMessageResponse;
import com.cargoapp.backend.chat.dto.ChatRoomResponse;
import com.cargoapp.backend.chat.entity.ChatMessageEntity;
import com.cargoapp.backend.chat.entity.ChatRoomEntity;
import com.cargoapp.backend.chat.entity.SenderType;
import com.cargoapp.backend.managers.repository.ManagerRepository;
import com.cargoapp.backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChatMapper {

    private final UserRepository userRepository;
    private final ManagerRepository managerRepository;

    public String resolveSenderName(SenderType senderType, UUID senderId) {
        if (senderType == SenderType.USER) {
            return userRepository.findById(senderId)
                    .map(u -> u.getFirstName() + " " + u.getLastName())
                    .orElse("Удалённый пользователь");
        }
        return managerRepository.findById(senderId)
                .map(m -> m.getFirstName() + " " + m.getLastName())
                .orElse("Удалённый менеджер");
    }

    public ChatMessageResponse toMessageResponse(ChatMessageEntity message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getChatRoom().getId(),
                message.getSenderType(),
                message.getSenderId(),
                resolveSenderName(message.getSenderType(), message.getSenderId()),
                message.getContent(),
                message.isRead(),
                message.getCreatedAt()
        );
    }

    public ChatRoomResponse toRoomResponse(ChatRoomEntity room, long unreadCount, ChatMessageResponse lastMessage) {
        return new ChatRoomResponse(
                room.getId(),
                room.getUser().getId(),
                room.getUser().getFirstName() + " " + room.getUser().getLastName(),
                room.getUser().getPersonalCode(),
                room.getBranch().getId(),
                room.getBranch().getAddress(),
                unreadCount,
                lastMessage,
                room.getCreatedAt()
        );
    }
}