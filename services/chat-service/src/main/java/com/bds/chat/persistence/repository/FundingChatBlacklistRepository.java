package com.bds.chat.persistence.repository;

import com.bds.chat.common.enums.BlacklistStatus;
import com.bds.chat.persistence.entity.FundingChatBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FundingChatBlacklistRepository extends JpaRepository<FundingChatBlacklist, Long> {

    boolean existsByRoomIdAndMemberIdAndStatus(Long roomId, Long memberId, BlacklistStatus status);

    Optional<FundingChatBlacklist> findByRoomIdAndMemberId(Long roomId, Long memberId);
}
