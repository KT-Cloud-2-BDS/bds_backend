package com.bds.chat.application.member.service;

import com.bds.chat.application.member.dto.InquiryMemberLeaveResponseDto;
import com.bds.chat.common.BusinessException;
import com.bds.chat.common.ErrorCode;
import com.bds.chat.domain.member.InquiryChatMember;
import com.bds.chat.domain.member.InquiryChatMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InquiryRoomMemberService {

    private final InquiryChatMemberRepository inquiryChatMemberRepository;
    private final Clock clock;

    // 1:1 문의 채팅방 나가기
    @Transactional
    public InquiryMemberLeaveResponseDto leave(Long roomId, Long memberId) {
        LocalDateTime now = LocalDateTime.now(clock);
        InquiryChatMember member = inquiryChatMemberRepository.findActiveMember(roomId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "roomId=" + roomId + " memberId=" + memberId));
        member.leave(now);
        InquiryChatMember saved = inquiryChatMemberRepository.save(member);
        return InquiryMemberLeaveResponseDto.from(saved);
    }

    // 1:1 문의 채팅방 재입장
    @Transactional
    public void rejoin(Long roomId, Long memberId) {
        LocalDateTime now = LocalDateTime.now(clock);
        InquiryChatMember member = inquiryChatMemberRepository.findByRoomIdAndMemberId(roomId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "roomId=" + roomId + " memberId=" + memberId));
        member.rejoin(now);
        inquiryChatMemberRepository.save(member);
    }

}
