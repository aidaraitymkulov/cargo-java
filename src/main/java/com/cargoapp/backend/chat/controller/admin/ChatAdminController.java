package com.cargoapp.backend.chat.controller.admin;

import com.cargoapp.backend.chat.dto.ChatMessageResponse;
import com.cargoapp.backend.chat.dto.ChatRoomResponse;
import com.cargoapp.backend.chat.dto.UnreadCountResponse;
import com.cargoapp.backend.chat.entity.ChatRoomEntity;
import com.cargoapp.backend.chat.entity.SenderType;
import com.cargoapp.backend.chat.service.ChatMessageService;
import com.cargoapp.backend.chat.service.ChatRoomService;
import com.cargoapp.backend.common.annotation.CurrentUserId;
import com.cargoapp.backend.common.dto.PagedResponse;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/admin/chat")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
public class ChatAdminController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    @GetMapping("/rooms")
    public List<ChatRoomResponse> getRooms(@CurrentUserId UUID managerId) {
        return chatRoomService.getRoomsForManager(managerId);
    }

    @GetMapping("/rooms/{roomId}/messages")
    public PagedResponse<ChatMessageResponse> getMessages(
            @CurrentUserId UUID managerId,
            @PathVariable UUID roomId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int pageSize
    ) {
        ChatRoomEntity room = chatRoomService.getRoomById(roomId);
        chatRoomService.validateManagerAccess(room, managerId);
        return chatMessageService.getMessages(roomId, page, pageSize);
    }

    @PostMapping("/rooms/{roomId}/messages/read")
    public void markAsRead(@CurrentUserId UUID managerId, @PathVariable UUID roomId) {
        ChatRoomEntity room = chatRoomService.getRoomById(roomId);
        chatRoomService.validateManagerAccess(room, managerId);
        chatMessageService.markAsRead(roomId, SenderType.MANAGER);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse getUnreadCount(@CurrentUserId UUID managerId) {
        return chatMessageService.getUnreadCountForManager(managerId);
    }
}