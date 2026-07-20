package com.bds.common.events.chat;

import com.bds.common.events.PublishTo;

import java.util.UUID;

@PublishTo(exchange = "chat.exchange", routingKey = "chat.send")
public record ChatSendEvent
        (
        UUID eventId,
        Long roomId,
        Long memberId,
        String Content
) {
    public static ChatSendEvent of(Long roomId, Long memberId, String Content) {
        return new ChatSendEvent(
                UUID.randomUUID(),roomId,memberId,Content
        );
    }
}
