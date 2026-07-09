package com.bds.chat.application.message.dto;

import java.time.Instant;

public record ReadReceiptDto(Long roomId, Long userId, Long lastReadMessageId, Instant readAt) {}
