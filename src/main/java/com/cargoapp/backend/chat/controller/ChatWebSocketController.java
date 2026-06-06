package com.cargoapp.backend.chat.controller;

import com.cargoapp.backend.chat.dto.ChatMessageResponse;
import com.cargoapp.backend.chat.dto.MarkAsReadRequest;
import com.cargoapp.backend.chat.dto.SendMessageRequest;
import com.cargoapp.backend.chat.entity.SenderType;
import com.cargoapp.backend.chat.service.ChatMessageService;
import com.cargoapp.backend.chat.service.ChatRoomService;
import com.cargoapp.backend.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatMessageService chatMessageService;
    private final ChatRoomService chatRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(SendMessageRequest request, Principal principal) {
        var auth = (UsernamePasswordAuthenticationToken) principal;
        UUID senderId = UUID.fromString(auth.getName());
        SenderType senderType = isManager(auth) ? SenderType.MANAGER : SenderType.USER;

        ChatMessageResponse response = chatMessageService.sendMessage(
                senderId, senderType, request.roomId(), request.content());
        messagingTemplate.convertAndSend("/topic/chat.room." + request.roomId(), response);
    }

    @MessageMapping("/chat.read")
    public void markAsRead(MarkAsReadRequest request, Principal principal) {
        var auth = (UsernamePasswordAuthenticationToken) principal;
        UUID readerId = UUID.fromString(auth.getName());
        SenderType readerType = isManager(auth) ? SenderType.MANAGER : SenderType.USER;

        var room = chatRoomService.getRoomById(request.roomId());
        if (readerType == SenderType.USER) {
            chatRoomService.validateUserAccess(room, readerId);
        } else {
            chatRoomService.validateManagerAccess(room, readerId);
        }

        chatMessageService.markAsRead(request.roomId(), readerType);
    }

    @MessageExceptionHandler(AppException.class)
    @SendToUser("/queue/errors")
    public Map<String, String> handleAppException(AppException ex) {
        return Map.of(
                "error", ex.getErrorCode(),
                "message", ex.getMessage()
        );
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public Map<String, String> handleException(Exception ex) {
        log.error("WebSocket error", ex);
        return Map.of(
                "error", "INTERNAL_ERROR",
                "message", "Внутренняя ошибка сервера"
        );
    }

    private boolean isManager(UsernamePasswordAuthenticationToken auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a != null && a.startsWith("ROLE_"));
    }
}