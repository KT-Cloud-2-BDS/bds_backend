package com.bds.common.events.order;

import org.springframework.modulith.events.Externalized;

import java.time.Instant;
import java.util.UUID;

@Externalized("order.exchange::order.created")
public record OrderCreatedEvent(
        UUID eventId,
        Long orderId,
        Long amount,
        Instant occurredAt
) {
    public static OrderCreatedEvent of(Long orderId, Long amount) {
        return new OrderCreatedEvent(UUID.randomUUID(), orderId, amount, Instant.now());
    }
}
