package com.bds.chat.presentation.stomp.dto;

import java.time.Instant;

public record ChatMessageDto(
        String messageId,   // 클라이언트 발급 UUID(clientMessageId) — 재전송 dedupe·echo-ack 키
        Long seq,           // DB PK — 저장 후 채움, 정렬 키
        Long roomId,
        String senderId,
        String content,
        Instant sentAt
) {
    public ChatMessageDto(String messageId, Long roomId, String senderId, String content, Instant sentAt) {
        this(messageId, null, roomId, senderId, content, sentAt);
    }

    public ChatMessageDto withSeq(Long seq) {
        return new ChatMessageDto(messageId, seq, roomId, senderId, content, sentAt);
    }
}
