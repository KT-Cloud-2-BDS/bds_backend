package com.bds.chat.domain.blackList;

import java.util.Optional;

public interface FundingChatBlacklistRepository {

    boolean isBlacklisted(Long roomId, Long memberId);

    Optional<FundingChatBlacklist> findBlacklist(Long roomId, Long memberId);

    FundingChatBlacklist save(FundingChatBlacklist blacklist);
}
