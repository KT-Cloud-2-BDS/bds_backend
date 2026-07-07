package com.bds.chat.infrastructure.persistence.message;

import com.bds.chat.domain.message.ChatMessage;
import com.bds.chat.domain.shared.ChatMessageId;
import com.bds.chat.domain.shared.ChatRoomId;
import com.bds.chat.domain.shared.MemberId;
import com.bds.chat.infrastructure.persistence.chatroom.ChatRoomJpaEntity;
import org.springframework.stereotype.Component;

@Component
class ChatMessageMapper {

    ChatMessage toDomain(ChatMessageJpaEntity entity) {
        return ChatMessage.restore(
                ChatMessageId.of(entity.getId()),
                ChatRoomId.of(entity.getRoom().getId()),
                entity.getSenderId() != null ? MemberId.of(entity.getSenderId()) : null,
                entity.getContent(),
                entity.getType(),
                entity.getClientId(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getDeletedAt()
        );
    }

    ChatMessageJpaEntity toJpaEntity(ChatMessage domain, ChatRoomJpaEntity roomRef) {
        return ChatMessageJpaEntity.builder()
                .id(domain.getId() != null ? domain.getId().value() : null)
                .room(roomRef)
                .senderId(domain.getSenderId() != null ? domain.getSenderId().value() : null)
                .content(domain.getContent())
                .type(domain.getType())
                .status(domain.getStatus())
                .deletedAt(domain.getDeletedAt())
                .clientId(domain.getClientId())
                .build();
    }
}
