package com.bds.chat.infrastructure.persistence.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface InquiryChatMemberJpaRepository extends JpaRepository<InquiryChatMemberJpaEntity, Long> {

    Optional<InquiryChatMemberJpaEntity> findByRoom_IdAndMemberIdAndDeletedAtIsNull(Long roomId, Long memberId);

    List<InquiryChatMemberJpaEntity> findByRoom_IdAndDeletedAtIsNull(Long roomId);
}
