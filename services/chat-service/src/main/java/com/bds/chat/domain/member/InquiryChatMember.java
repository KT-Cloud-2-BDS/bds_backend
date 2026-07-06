package com.bds.chat.domain.member;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InquiryChatMember {

    private Long id;
    private Long roomId;
    private Long memberId;
    private MemberStatus status;
    private Long lastReadMessageId;
    private LocalDateTime joinedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public void updateLastRead(Long messageId) {
        this.lastReadMessageId = messageId;
        this.updatedAt = LocalDateTime.now();
    }

    public void leave() {
        if(this.status == MemberStatus.BANNED ||  this.status == MemberStatus.LEFT) {
            return;
        }
        this.status = MemberStatus.LEFT;
        this.deletedAt = LocalDateTime.now();
    }
}
