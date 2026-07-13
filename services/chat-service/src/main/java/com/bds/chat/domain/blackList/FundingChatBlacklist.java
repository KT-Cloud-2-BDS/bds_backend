package com.bds.chat.domain.blackList;

import com.bds.chat.common.BusinessException;
import com.bds.chat.common.ErrorCode;
import com.bds.chat.domain.shared.ChatRoomId;
import com.bds.chat.domain.shared.FundingChatBlacklistId;
import com.bds.chat.domain.shared.MemberId;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
public class FundingChatBlacklist {

    private FundingChatBlacklistId id;
    private final ChatRoomId roomId;
    private final MemberId memberId;
    private final String reason;
    private BlacklistStatus status;
    private final LocalDateTime bannedAt;
    private LocalDateTime deletedAt;

    private FundingChatBlacklist(FundingChatBlacklistId id,
                                  ChatRoomId roomId,
                                  MemberId memberId,
                                  String reason,
                                  BlacklistStatus status,
                                  LocalDateTime bannedAt,
                                  LocalDateTime deletedAt) {
        this.id = id;
        this.roomId = Objects.requireNonNull(roomId, "roomId");
        this.memberId = Objects.requireNonNull(memberId, "memberId");
        this.reason = reason;
        this.status = Objects.requireNonNull(status, "status");
        this.bannedAt = Objects.requireNonNull(bannedAt, "bannedAt");
        this.deletedAt = deletedAt;
    }

    public static FundingChatBlacklist create(ChatRoomId roomId, MemberId memberId, String reason, LocalDateTime now) {
        return new FundingChatBlacklist(null, roomId, memberId, reason, BlacklistStatus.ACTIVE, now, null);
    }

    public static FundingChatBlacklist restore(FundingChatBlacklistId id,
                                               ChatRoomId roomId,
                                               MemberId memberId,
                                               String reason,
                                               BlacklistStatus status,
                                               LocalDateTime bannedAt,
                                               LocalDateTime deletedAt) {
        Objects.requireNonNull(id, "id");
        return new FundingChatBlacklist(id, roomId, memberId, reason, status, bannedAt, deletedAt);
    }

    public void assignId(FundingChatBlacklistId id) {
        if (this.id != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "FundingChatBlacklistId already assigned");
        }
        this.id = Objects.requireNonNull(id, "id");
    }

    public void release(LocalDateTime now) {
        if (this.status == BlacklistStatus.RELEASED) {
            return;
        }
        this.status = BlacklistStatus.RELEASED;
        this.deletedAt = now;
    }
}
