package com.bds.chat.application.chatRoom.dto;

import com.bds.chat.domain.member.InquiryChatMember;

public record ParticipantDto(
        Long memberId,
        MembershipStatusDto membership
) {
    public static ParticipantDto from(InquiryChatMember member) {
        return new ParticipantDto(
                member.getMemberId().value(),
                MembershipStatusDto.from(member)
        );
    }
}
