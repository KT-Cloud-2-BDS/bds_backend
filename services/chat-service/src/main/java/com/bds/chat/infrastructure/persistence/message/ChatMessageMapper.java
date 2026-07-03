package com.bds.chat.infrastructure.persistence.message;

import com.bds.chat.domain.message.ChatMessage;
import com.bds.chat.domain.message.MessageStatus;
import com.bds.chat.infrastructure.persistence.chatroom.ChatRoomJpaEntity;
import org.springframework.stereotype.Component;

@Component
class ChatMessageMapper {

    ChatMessage toDomain(ChatMessageJpaEntity entity) {
        return ChatMessage.builder()
                .id(entity.getId())
                .roomId(entity.getRoom().getId())
                .senderId(entity.getSenderId())
                .content(entity.getContent())
                .type(entity.getType())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .deletedAt(entity.getDeletedAt())
                .clientId(entity.getClientId())
                .build();
    }

    ChatMessageJpaEntity toJpaEntity(ChatMessage domain, ChatRoomJpaEntity roomRef) {
        return ChatMessageJpaEntity.builder()
                .id(domain.getId())
                .room(roomRef)
                .senderId(domain.getSenderId())
                .content(domain.getContent())
                .type(domain.getType())
                .status(domain.getStatus() != null ? domain.getStatus() : MessageStatus.SENT)
                .deletedAt(domain.getDeletedAt())
                .clientId(domain.getClientId())
                .build();
    }
}
