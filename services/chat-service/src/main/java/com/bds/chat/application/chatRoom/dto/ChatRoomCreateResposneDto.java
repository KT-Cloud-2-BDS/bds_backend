package com.bds.chat.application.chatRoom.dto;

import com.bds.chat.domain.chatRoom.ChatRoom;

public record ChatRoomCreateResposneDto(
        Long roomId,
        Long productId,
        String title,
        String type,
        String status
) {
    public static ChatRoomCreateResposneDto from(ChatRoom room) {
        return new ChatRoomCreateResposneDto(
                room.getId().value(),
                room.getProductId().value(),
                room.getTitle(),
                room.getType().name(),
                room.getStatus().name()
        );
    }
}
