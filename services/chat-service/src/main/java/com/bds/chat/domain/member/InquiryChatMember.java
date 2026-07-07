package com.bds.chat.domain.member;

import com.bds.chat.common.BusinessException;
import com.bds.chat.common.ErrorCode;
import com.bds.chat.domain.shared.ChatRoomId;
import com.bds.chat.domain.shared.InquiryChatMemberId;
import com.bds.chat.domain.shared.MemberId;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
public class InquiryChatMember {

    private InquiryChatMemberId id;
    private final ChatRoomId roomId;
    private final MemberId memberId;
    private MemberStatus status;
    private Long lastReadMessageId;
    private final LocalDateTime joinedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    private InquiryChatMember(InquiryChatMemberId id,
                               ChatRoomId roomId,
                               MemberId memberId,
                               MemberStatus status,
                               Long lastReadMessageId,
                               LocalDateTime joinedAt,
                               LocalDateTime updatedAt,
                               LocalDateTime deletedAt) {
        this.id = id;
        this.roomId = Objects.requireNonNull(roomId, "roomId");
        this.memberId = Objects.requireNonNull(memberId, "memberId");
        this.status = Objects.requireNonNull(status, "status");
        this.lastReadMessageId = lastReadMessageId;
        this.joinedAt = Objects.requireNonNull(joinedAt, "joinedAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.deletedAt = deletedAt;
    }

    public static InquiryChatMember create(ChatRoomId roomId, MemberId memberId, LocalDateTime now) {
        return new InquiryChatMember(null, roomId, memberId, MemberStatus.ACTIVE, null, now, now, null);
    }

    public static InquiryChatMember restore(InquiryChatMemberId id,
                                            ChatRoomId roomId,
                                            MemberId memberId,
                                            MemberStatus status,
                                            Long lastReadMessageId,
                                            LocalDateTime joinedAt,
                                            LocalDateTime updatedAt,
                                            LocalDateTime deletedAt) {
        Objects.requireNonNull(id, "id");
        return new InquiryChatMember(id, roomId, memberId, status, lastReadMessageId, joinedAt, updatedAt, deletedAt);
    }

    public void assignId(InquiryChatMemberId id) {
        if (this.id != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "InquiryChatMemberId already assigned");
        }
        this.id = Objects.requireNonNull(id, "id");
    }

    public void rejoin(LocalDateTime now) {
        if (this.status == MemberStatus.BANNED) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Banned member cannot rejoin");
        }
        if (this.status == MemberStatus.ACTIVE) {
            return;
        }
        this.status = MemberStatus.ACTIVE;
        this.deletedAt = null;
        this.updatedAt = now;
    }

    public void leave(LocalDateTime now) {
        if (this.status == MemberStatus.LEFT) {
            return;
        }
        if (this.status == MemberStatus.BANNED) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Banned member cannot leave");
        }
        this.status = MemberStatus.LEFT;
        this.updatedAt = now;
        this.deletedAt = now;
    }

    public void ban(LocalDateTime now) {
        if (this.status == MemberStatus.BANNED) {
            return;
        }
        this.status = MemberStatus.BANNED;
        this.updatedAt = now;
        this.deletedAt = now;
    }

    public void updateLastRead(Long messageId, LocalDateTime now) {
        if (!isActive()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Only active member can update last read");
        }
        if (this.lastReadMessageId != null && messageId <= this.lastReadMessageId) {
            return;
        }
        this.lastReadMessageId = messageId;
        this.updatedAt = now;
    }

    public boolean isActive() { return status == MemberStatus.ACTIVE; }
    public boolean isLeft()   { return status == MemberStatus.LEFT; }
    public boolean isBanned() { return status == MemberStatus.BANNED; }

    public boolean belongsTo(ChatRoomId roomId) { return this.roomId.equals(roomId); }
    public boolean is(MemberId memberId)         { return this.memberId.equals(memberId); }
}
