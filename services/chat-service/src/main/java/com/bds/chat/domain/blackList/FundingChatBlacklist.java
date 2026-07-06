package com.bds.chat.domain.blackList;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FundingChatBlacklist {

    private Long id;
    private Long roomId;
    private Long memberId;
    private String reason;
    private BlacklistStatus status;
    private LocalDateTime bannedAt;
    private LocalDateTime deletedAt;

    public void release() {
        if(this.status == BlacklistStatus.RELEASED){
            return;
        }
        this.status = BlacklistStatus.RELEASED;
        this.deletedAt = LocalDateTime.now();
    }
}
