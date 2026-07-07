package com.bds.chat.domain.chatRoom;

import com.bds.chat.common.BusinessException;
import com.bds.chat.common.ErrorCode;
import com.bds.chat.domain.shared.ChatRoomId;
import com.bds.chat.domain.shared.MemberId;
import com.bds.chat.domain.shared.ProductId;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
public class ChatRoom {

    private ChatRoomId id;
    private final MemberId creatorId;
    private final ProductId productId;
    private final String title;
    private ChatRoomStatus status;
    private final ChatRoomType type;
    private final LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    private ChatRoom(ChatRoomId id,
                     MemberId creatorId,
                     ProductId productId,
                     String title,
                     ChatRoomStatus status,
                     ChatRoomType type,
                     LocalDateTime createdAt,
                     LocalDateTime deletedAt) {
        this.id = id;
        this.creatorId = Objects.requireNonNull(creatorId, "creatorId");
        this.productId = Objects.requireNonNull(productId, "productId");
        this.title = title;
        this.status = Objects.requireNonNull(status, "status");
        this.type = Objects.requireNonNull(type, "type");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.deletedAt = deletedAt;
    }

    public static ChatRoom create(MemberId creatorId,
                                  ProductId productId,
                                  String title,
                                  ChatRoomType type,
                                  LocalDateTime now) {
        return new ChatRoom(null, creatorId, productId, title, ChatRoomStatus.ACTIVE, type, now, null);
    }

    public static ChatRoom restore(ChatRoomId id,
                                   MemberId creatorId,
                                   ProductId productId,
                                   String title,
                                   ChatRoomStatus status,
                                   ChatRoomType type,
                                   LocalDateTime createdAt,
                                   LocalDateTime deletedAt) {
        Objects.requireNonNull(id, "id");
        return new ChatRoom(id, creatorId, productId, title, status, type, createdAt, deletedAt);
    }

    public void assignId(ChatRoomId id) {
        if (this.id != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "ChatRoomId already assigned");
        }
        this.id = Objects.requireNonNull(id, "id");
    }

    public void delete(LocalDateTime now) {
        if (this.status == ChatRoomStatus.CLOSED) {
            return;
        }
        ensureActive();
        this.status = ChatRoomStatus.CLOSED;
        this.deletedAt = now;
    }

    private void ensureActive() {
        if (this.status != ChatRoomStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CONFLICT, "ChatRoom is not active: status=" + status);
        }
    }
}
