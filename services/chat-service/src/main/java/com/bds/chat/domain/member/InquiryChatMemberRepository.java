package com.bds.chat.domain.member;

import com.bds.chat.application.message.dto.ReadReceiptDto;

import java.util.List;
import java.util.Optional;

public interface InquiryChatMemberRepository {

    Optional<InquiryChatMember> findActiveMember(Long roomId, Long memberId);

    boolean existsActiveMember(Long roomId, Long memberId);

    Optional<InquiryChatMember> findByRoomIdAndMemberId(Long roomId, Long memberId);

    List<InquiryChatMember> findAllByRoomId(Long roomId);

    List<InquiryChatMember> findActiveMembers(Long roomId);

    List<InquiryChatMember> findByMemberId(Long memberId);

    List<InquiryChatMember> findActiveMembersByRoomIds(List<Long> roomIds);

    InquiryChatMember save(InquiryChatMember member);

    void bulkUpdateLastRead(List<ReadReceiptDto> batch);
}
