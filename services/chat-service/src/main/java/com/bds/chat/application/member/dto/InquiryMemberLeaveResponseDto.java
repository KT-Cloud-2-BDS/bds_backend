package com.bds.chat.application.member.dto;

import com.bds.chat.domain.member.InquiryChatMember;

import java.time.LocalDateTime;

public record InquiryMemberLeaveResponseDto(
        Long roomId,
        Long memberId,
        LocalDateTime leftAt
) {
    public static InquiryMemberLeaveResponseDto from(InquiryChatMember member) {
        return new InquiryMemberLeaveResponseDto(
                member.getRoomId().value(),
                member.getMemberId().value(),
                member.getDeletedAt()
        );
    }
}
