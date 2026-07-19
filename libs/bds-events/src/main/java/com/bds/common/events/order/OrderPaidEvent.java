package com.bds.common.events.order;


import org.springframework.modulith.events.Externalized;

import java.time.Instant;
import java.util.UUID;

@Externalized("order.exchange::order.process.paid")
public record OrderPaidEvent(
        UUID eventId,
        Long orderId,
        Instant occurredAt
) {
    public static OrderPaidEvent of(Long orderId) {
        return new OrderPaidEvent(UUID.randomUUID(), orderId, Instant.now());
    }
}