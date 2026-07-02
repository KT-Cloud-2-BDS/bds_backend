package com.bds.chat.infrastructure.persistence.chatroom;

import com.bds.chat.domain.chatRoom.ChatRoom;
import org.springframework.stereotype.Component;

@Component
class ChatRoomMapper {

    ChatRoom toDomain(ChatRoomJpaEntity entity) {
        return ChatRoom.builder()
                .id(entity.getId())
                .creatorId(entity.getCreatorId())
                .productId(entity.getProductId())
                .title(entity.getTitle())
                .status(entity.getStatus())
                .type(entity.getType())
                .createdAt(entity.getCreatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    ChatRoomJpaEntity toJpaEntity(ChatRoom domain) {
        return ChatRoomJpaEntity.builder()
                .id(domain.getId())
                .creatorId(domain.getCreatorId())
                .productId(domain.getProductId())
                .title(domain.getTitle())
                .status(domain.getStatus())
                .type(domain.getType())
                .createdAt(domain.getCreatedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }
}
