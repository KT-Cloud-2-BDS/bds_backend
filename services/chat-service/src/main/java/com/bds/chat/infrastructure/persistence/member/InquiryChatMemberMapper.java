package com.bds.chat.infrastructure.persistence.member;

import com.bds.chat.domain.member.InquiryChatMember;
import com.bds.chat.infrastructure.persistence.chatroom.ChatRoomJpaEntity;
import org.springframework.stereotype.Component;

@Component
class InquiryChatMemberMapper {

    InquiryChatMember toDomain(InquiryChatMemberJpaEntity entity) {
        return InquiryChatMember.builder()
                .id(entity.getId())
                .roomId(entity.getRoom().getId())
                .memberId(entity.getMemberId())
                .status(entity.getStatus())
                .lastReadMessageId(entity.getLastReadMessageId())
                .joinedAt(entity.getJoinedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    InquiryChatMemberJpaEntity toJpaEntity(InquiryChatMember domain, ChatRoomJpaEntity roomRef) {
        return InquiryChatMemberJpaEntity.builder()
                .id(domain.getId())
                .room(roomRef)
                .memberId(domain.getMemberId())
                .status(domain.getStatus())
                .lastReadMessageId(domain.getLastReadMessageId())
                .joinedAt(domain.getJoinedAt())
                .updatedAt(domain.getUpdatedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }
}
