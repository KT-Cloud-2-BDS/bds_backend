package com.bds.chat.persistence.repository;

import com.bds.chat.persistence.entity.InquiryChatMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InquiryChatMemberRepository extends JpaRepository<InquiryChatMember, Long> {

    Optional<InquiryChatMember> findByRoomIdAndMemberIdAndDeletedAtIsNull(Long roomId, Long memberId);

    List<InquiryChatMember> findByRoomIdAndDeletedAtIsNull(Long roomId);
}
