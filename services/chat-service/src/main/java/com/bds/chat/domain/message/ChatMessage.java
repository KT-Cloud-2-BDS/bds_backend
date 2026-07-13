package com.bds.chat.domain.message;

import com.bds.chat.common.BusinessException;
import com.bds.chat.common.ErrorCode;
import com.bds.chat.domain.shared.ChatMessageId;
import com.bds.chat.domain.shared.ChatRoomId;
import com.bds.chat.domain.shared.MemberId;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
public class ChatMessage {

    private ChatMessageId id;
    private final ChatRoomId roomId;
    private final MemberId senderId;    // SYSTEM 메시지의 경우 null
    private final String content;
    private final MessageType type;
    private final String clientId;
    private MessageStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    private ChatMessage(ChatMessageId id,
                        ChatRoomId roomId,
                        MemberId senderId,
                        String content,
                        MessageType type,
                        String clientId,
                        MessageStatus status,
                        LocalDateTime createdAt,
                        LocalDateTime deletedAt) {
        this.id = id;
        this.roomId = Objects.requireNonNull(roomId, "roomId");
        this.senderId = senderId;
        this.content = Objects.requireNonNull(content, "content");
        this.type = Objects.requireNonNull(type, "type");
        this.clientId = clientId;
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.deletedAt = deletedAt;
    }

    public static ChatMessage create(ChatRoomId roomId,
                                     MemberId senderId,
                                     String content,
                                     MessageType type,
                                     String clientId,
                                     LocalDateTime now) {
        return new ChatMessage(null, roomId, senderId, content, type, clientId, MessageStatus.SENT, now, null);
    }

    public static ChatMessage createSystem(ChatRoomId roomId, String content, LocalDateTime now) {
        return new ChatMessage(null, roomId, null, content, MessageType.SYSTEM, null, MessageStatus.SENT, now, null);
    }

    public static ChatMessage restore(ChatMessageId id,
                                      ChatRoomId roomId,
                                      MemberId senderId,
                                      String content,
                                      MessageType type,
                                      String clientId,
                                      MessageStatus status,
                                      LocalDateTime createdAt,
                                      LocalDateTime deletedAt) {
        Objects.requireNonNull(id, "id");
        return new ChatMessage(id, roomId, senderId, content, type, clientId, status, createdAt, deletedAt);
    }

    public void assignId(ChatMessageId id) {
        if (this.id != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "ChatMessageId already assigned");
        }
        this.id = Objects.requireNonNull(id, "id");
    }

    public void delete(LocalDateTime now) {
        if (this.status == MessageStatus.DELETED) {
            return;
        }
        this.status = MessageStatus.DELETED;
        this.deletedAt = now;
    }

    public boolean isDeleted() { return status == MessageStatus.DELETED; }
}
