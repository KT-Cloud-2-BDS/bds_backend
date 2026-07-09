package com.bds.chat.infrastructure.persistence.blacklist;

import com.bds.chat.domain.blackList.FundingChatBlacklist;
import com.bds.chat.infrastructure.persistence.chatroom.ChatRoomJpaEntity;
import org.springframework.stereotype.Component;

@Component
class FundingChatBlacklistMapper {

    FundingChatBlacklist toDomain(FundingChatBlacklistJpaEntity entity) {
        return FundingChatBlacklist.builder()
                .id(entity.getId())
                .roomId(entity.getRoom().getId())
                .memberId(entity.getMemberId())
                .reason(entity.getReason())
                .status(entity.getStatus())
                .bannedAt(entity.getBannedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    FundingChatBlacklistJpaEntity toJpaEntity(FundingChatBlacklist domain, ChatRoomJpaEntity roomRef) {
        return FundingChatBlacklistJpaEntity.builder()
                .id(domain.getId())
                .room(roomRef)
                .memberId(domain.getMemberId())
                .reason(domain.getReason())
                .status(domain.getStatus())
                .bannedAt(domain.getBannedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }
}
