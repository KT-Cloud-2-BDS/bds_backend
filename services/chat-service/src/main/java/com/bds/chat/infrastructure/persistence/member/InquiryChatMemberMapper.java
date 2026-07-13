package com.bds.chat.infrastructure.persistence.member;

import com.bds.chat.domain.member.InquiryChatMember;
import com.bds.chat.domain.shared.ChatRoomId;
import com.bds.chat.domain.shared.InquiryChatMemberId;
import com.bds.chat.domain.shared.MemberId;
import com.bds.chat.infrastructure.persistence.chatroom.ChatRoomJpaEntity;
import org.springframework.stereotype.Component;

@Component
class InquiryChatMemberMapper {

    InquiryChatMember toDomain(InquiryChatMemberJpaEntity entity) {
        return InquiryChatMember.restore(
                InquiryChatMemberId.of(entity.getId()),
                ChatRoomId.of(entity.getRoom().getId()),
                MemberId.of(entity.getMemberId()),
                entity.getStatus(),
                entity.getLastReadMessageId(),
                entity.getJoinedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt()
        );
    }

    InquiryChatMemberJpaEntity toJpaEntity(InquiryChatMember domain, ChatRoomJpaEntity roomRef) {
        return InquiryChatMemberJpaEntity.builder()
                .id(domain.getId() != null ? domain.getId().value() : null)
                .room(roomRef)
                .memberId(domain.getMemberId().value())
                .status(domain.getStatus())
                .lastReadMessageId(domain.getLastReadMessageId())
                .joinedAt(domain.getJoinedAt())
                .updatedAt(domain.getUpdatedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }
}
