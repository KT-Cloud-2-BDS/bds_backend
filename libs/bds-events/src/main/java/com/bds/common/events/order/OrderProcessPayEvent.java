package com.bds.common.events.order;

import org.springframework.modulith.events.Externalized;

import java.util.UUID;

@Externalized("order.exchange::order.pay.requested")
public record OrderProcessPayEvent(
        UUID requestId,
        Long orderId,
        Long memberId,
        Long fundingId,
        Long amount,
        String paymentType
) {
    public static OrderProcessPayEvent of(Long orderId, Long memberId, Long fundingId, Long amount) {
        return new OrderProcessPayEvent(
                UUID.randomUUID(),
                orderId,
                memberId,
                fundingId,
                amount,
                "INSTANT"
        );
    }
}