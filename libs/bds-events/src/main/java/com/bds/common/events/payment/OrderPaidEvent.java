package com.bds.common.events.payment;


import org.springframework.modulith.events.Externalized;

import java.time.Instant;
import java.util.UUID;

@Externalized("payment.exchange::payment.paid")
public record OrderPaidEvent(
        UUID eventId,
        Long orderId,
        Instant occurredAt
) {
    public static OrderPaidEvent of(Long orderId) {
        return new OrderPaidEvent(UUID.randomUUID(), orderId, Instant.now());
    }
}