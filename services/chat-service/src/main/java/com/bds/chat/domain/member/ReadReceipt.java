package com.bds.chat.domain.member;

import java.time.Instant;

public record ReadReceipt(Long roomId, Long userId, Long lastReadMessageId, Instant readAt) {}
