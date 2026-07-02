package com.bds.chat.domain.message;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatMessage {

    private Long id;
    private Long roomId;
    private Long senderId;
    private String content;
    private MessageStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
    private String clientId;

    public void delete() {
        this.status = MessageStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }
}
