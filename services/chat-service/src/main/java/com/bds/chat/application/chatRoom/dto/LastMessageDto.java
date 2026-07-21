package com.bds.chat.application.chatRoom.dto;

import com.bds.chat.domain.message.ChatMessage;

import java.time.LocalDateTime;

public record LastMessageDto(
        Long messageId,
        Long senderId,
        String content,
        boolean isDeleted,
        LocalDateTime createdAt
) {
    public static LastMessageDto from(ChatMessage message) {
        return new LastMessageDto(
                message.getId().value(),
                message.getSenderId() != null ? message.getSenderId().value() : null,
                message.getContent(),
                message.isDeleted(),
                message.getCreatedAt()
        );
    }
}
