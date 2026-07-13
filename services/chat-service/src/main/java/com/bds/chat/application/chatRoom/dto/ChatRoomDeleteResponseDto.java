package com.bds.chat.application.chatRoom.dto;

import java.time.LocalDateTime;

public record ChatRoomDeleteResponseDto(
        Long roomId,
        Boolean isDeleted,
        LocalDateTime deletedAt
) {
}
