package com.bds.chat.presentation.chatRoom.response;

import java.time.LocalDateTime;

public record ChatMessageResponseDto(
        Long id,
        Long roomId,
        Long senderId,
        String content,
        LocalDateTime createdAt
) {
}
