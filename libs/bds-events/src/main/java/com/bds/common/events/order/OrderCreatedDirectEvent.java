package com.bds.common.events.order;

import com.bds.common.events.PublishTo;

import java.time.Instant;
import java.util.UUID;

//DIRECT 전용 이벤트 예시
@PublishTo(exchange = "order.exchange", routingKey = "order.created")
public record OrderCreatedDirectEvent (
        UUID eventId,
        Long orderId,
        Long amount,
        Instant occurredAt
) {
    public static OrderCreatedDirectEvent of(Long orderId, Long amount) {
        return new OrderCreatedDirectEvent(UUID.randomUUID(), orderId, amount, Instant.now());
    }
}
