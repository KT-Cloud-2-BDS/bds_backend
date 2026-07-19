package com.bds.common.events.order;

import org.springframework.modulith.events.Externalized;

import java.time.Instant;
import java.util.UUID;

@Externalized("order.exchange::order.process.cancel")
public record OrderCancelledEvent(
        UUID eventId,
        Long orderId,
        String cancelReason,
        Instant occurredAt
) {
    public static OrderCancelledEvent of(Long orderId, String cancelReason) {
        return new OrderCancelledEvent(UUID.randomUUID(), orderId, cancelReason, Instant.now());
    }
}