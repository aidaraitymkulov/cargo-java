package com.cargoapp.backend.chat.mapper;

import com.cargoapp.backend.chat.dto.ChatMessageResponse;
import com.cargoapp.backend.chat.dto.ChatRoomResponse;
import com.cargoapp.backend.chat.entity.ChatMessageEntity;
import com.cargoapp.backend.chat.entity.ChatRoomEntity;
import org.springframework.stereotype.Component;

@Component
public class ChatMapper {

    public ChatMessageResponse toMessageResponse(ChatMessageEntity message, String senderName) {
        return new ChatMessageResponse(
                message.getId(),
                message.getChatRoom().getId(),
                message.getSenderType().name(),
                message.getSenderId(),
                senderName,
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