package com.bds.common.events.payment;

import org.springframework.modulith.events.Externalized;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Externalized("payment.exchange::order.process")
public record OrderProcessEvent(
        UUID eventId,
        List<Long> orderIds,
        String type,           // "CONFIRMED" or "REFUNDED"
        Instant occurredAt
) {
    public static OrderProcessEvent confirmed(List<Long> orderIds) {
        return new OrderProcessEvent(UUID.randomUUID(), orderIds, "CONFIRMED", Instant.now());
    }

    public static OrderProcessEvent refunded(List<Long> orderIds) {
        return new OrderProcessEvent(UUID.randomUUID(), orderIds, "REFUNDED", Instant.now());
    }
}