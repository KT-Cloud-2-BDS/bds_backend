package com.bds.chat.application.message.dto;

import com.bds.chat.domain.message.ChatMessage;

import java.time.LocalDateTime;

public record MessageDeleteResponseDto(
        Long messageId,
        boolean isDeleted,
        LocalDateTime deletedAt
) {
    public static MessageDeleteResponseDto from(ChatMessage msg) {
        return new MessageDeleteResponseDto(msg.getId().value(), msg.isDeleted(), msg.getDeletedAt());
    }
}
