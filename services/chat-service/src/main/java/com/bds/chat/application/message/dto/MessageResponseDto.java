package com.bds.chat.application.message.dto;

import com.bds.chat.domain.message.ChatMessage;

import java.time.LocalDateTime;

public record MessageResponseDto(
        Long messageId,
        Long senderId,
        String content,
        boolean isDeleted,
        LocalDateTime createdAt,
        Long roomId
) {
    public static MessageResponseDto from(ChatMessage msg) {
        return new MessageResponseDto(
                msg.getId().value(),
                msg.getSenderId() != null ? msg.getSenderId().value() : null,
                msg.getContent(),
                msg.isDeleted(),
                msg.getCreatedAt(),
                msg.getRoomId().value()
        );
    }
}
