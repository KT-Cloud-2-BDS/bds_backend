package com.bds.chat.infrastructure.persistence.message;

import java.time.LocalDateTime;

interface MessageWithUnreadProjection {
    Long getMessageId();
    Long getRoomId();
    Long getSenderId();
    String getContent();
    String getType();
    String getClientId();
    String getStatus();
    LocalDateTime getCreatedAt();
    Long getUnreadCount();
}
