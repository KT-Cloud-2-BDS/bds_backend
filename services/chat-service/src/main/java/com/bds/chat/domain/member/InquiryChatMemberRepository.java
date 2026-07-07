package com.bds.chat.domain.member;

import java.util.List;
import java.util.Optional;

public interface InquiryChatMemberRepository {

    Optional<InquiryChatMember> findActiveMember(Long roomId, Long memberId);

    List<InquiryChatMember> findActiveMembers(Long roomId);

    List<InquiryChatMember> findByMemberId(Long memberId);

    List<InquiryChatMember> findActiveMembersByRoomIds(List<Long> roomIds);

    InquiryChatMember save(InquiryChatMember member);
}
