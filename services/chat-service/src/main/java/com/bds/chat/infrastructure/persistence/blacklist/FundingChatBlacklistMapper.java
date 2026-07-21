package com.bds.chat.infrastructure.persistence.blacklist;

import com.bds.chat.domain.blackList.FundingChatBlacklist;
import com.bds.chat.domain.shared.ChatRoomId;
import com.bds.chat.domain.shared.FundingChatBlacklistId;
import com.bds.chat.domain.shared.MemberId;
import com.bds.chat.infrastructure.persistence.chatroom.ChatRoomJpaEntity;
import org.springframework.stereotype.Component;

@Component
class FundingChatBlacklistMapper {

    FundingChatBlacklist toDomain(FundingChatBlacklistJpaEntity entity) {
        return FundingChatBlacklist.restore(
                FundingChatBlacklistId.of(entity.getId()),
                ChatRoomId.of(entity.getRoom().getId()),
                MemberId.of(entity.getMemberId()),
                entity.getReason(),
                entity.getStatus(),
                entity.getBannedAt(),
                entity.getDeletedAt()
        );
    }

    FundingChatBlacklistJpaEntity toJpaEntity(FundingChatBlacklist domain, ChatRoomJpaEntity roomRef) {
        return FundingChatBlacklistJpaEntity.builder()
                .id(domain.getId() != null ? domain.getId().value() : null)
                .room(roomRef)
                .memberId(domain.getMemberId().value())
                .reason(domain.getReason())
                .status(domain.getStatus())
                .bannedAt(domain.getBannedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }
}
