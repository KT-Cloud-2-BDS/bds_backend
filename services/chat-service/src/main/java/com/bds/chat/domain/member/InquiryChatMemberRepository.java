package com.bds.chat.domain.member;

import java.util.List;
import java.util.Optional;

public interface InquiryChatMemberRepository {

    Optional<InquiryChatMember> findActiveMember(Long roomId, Long memberId);

    List<InquiryChatMember> findActiveMembers(Long roomId);

    InquiryChatMember save(InquiryChatMember member);
}
