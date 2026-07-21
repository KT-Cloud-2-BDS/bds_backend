package com.bds.chat.application.chatRoom.dto;

import com.bds.chat.domain.member.InquiryChatMember;

import java.time.LocalDateTime;

public record MembershipStatusDto(
        String status,
        Long lastReadMessageId,
        LocalDateTime joinedAt
) {
    public static MembershipStatusDto from(InquiryChatMember member) {
        return new MembershipStatusDto(
                member.getStatus().name(),
                member.getLastReadMessageId(),
                member.getJoinedAt()
        );
    }
}
