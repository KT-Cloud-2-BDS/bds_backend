package com.bds.chat.infrastructure.persistence.chatroom;

import com.bds.chat.domain.chatRoom.ChatRoom;
import com.bds.chat.domain.shared.ChatRoomId;
import com.bds.chat.domain.shared.MemberId;
import com.bds.chat.domain.shared.ProductId;
import org.springframework.stereotype.Component;

@Component
class ChatRoomMapper {

    ChatRoom toDomain(ChatRoomJpaEntity entity) {
        return ChatRoom.restore(
                ChatRoomId.of(entity.getId()),
                MemberId.of(entity.getCreatorId()),
                ProductId.of(entity.getProductId()),
                entity.getTitle(),
                entity.getStatus(),
                entity.getType(),
                entity.getCreatedAt(),
                entity.getDeletedAt()
        );
    }

    ChatRoomJpaEntity toJpaEntity(ChatRoom domain) {
        return ChatRoomJpaEntity.builder()
                .id(domain.getId() != null ? domain.getId().value() : null)
                .creatorId(domain.getCreatorId().value())
                .productId(domain.getProductId().value())
                .title(domain.getTitle())
                .status(domain.getStatus())
                .type(domain.getType())
                .createdAt(domain.getCreatedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }
}
