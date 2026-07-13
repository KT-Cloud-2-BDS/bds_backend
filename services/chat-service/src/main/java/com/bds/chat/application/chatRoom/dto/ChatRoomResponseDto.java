package com.bds.chat.application.chatRoom.dto;

import com.bds.chat.domain.chatRoom.ChatRoom;

import java.time.LocalDateTime;
import java.util.List;

public record ChatRoomResponseDto(
        Long roomId,
        String type,
        Long productId,
        List<Long> participants,
        Long createdBy,
        LocalDateTime createdAt,
        String status
) {
    public static ChatRoomResponseDto from(ChatRoom room, List<Long> participants) {
        return new ChatRoomResponseDto(
                room.getId().value(),
                room.getType().name(),
                room.getProductId().value(),
                participants,
                room.getCreatorId().value(),
                room.getCreatedAt(),
                room.getStatus().name()
        );
    }
}
