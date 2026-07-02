package com.bds.chat.infrastructure.persistence.blacklist;

import com.bds.chat.domain.blackList.BlacklistStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface FundingChatBlacklistJpaRepository extends JpaRepository<FundingChatBlacklistJpaEntity, Long> {

    boolean existsByRoom_IdAndMemberIdAndStatus(Long roomId, Long memberId, BlacklistStatus status);

    Optional<FundingChatBlacklistJpaEntity> findByRoom_IdAndMemberId(Long roomId, Long memberId);
}
