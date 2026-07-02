package com.bds.chat.domain.chatRoom;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatRoom {

    private Long id;
    private Long creatorId;
    private Long productId;
    private String title;
    private ChatRoomStatus status;
    private ChatRoomType type;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    public void close() {
        this.status = ChatRoomStatus.CLOSED;
        this.deletedAt = LocalDateTime.now();
    }
}
