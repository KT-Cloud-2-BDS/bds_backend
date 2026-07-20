package com.bds.common.events.order;

import com.bds.common.events.PublishTo;

import java.util.UUID;

@PublishTo(exchange = "payment.exchange", routingKey = "payment.process.refund")
public record PaymentProcessRefundEvent(
        String requestId,
        Long orderId,
        Long memberId,
        Long amount,
        String cancelReason
) {
    public static PaymentProcessRefundEvent of(Long orderId, Long memberId, Long amount, String cancelReason) {
        return new PaymentProcessRefundEvent(
                UUID.randomUUID().toString(),
                orderId,
                memberId,
                amount,
                cancelReason
        );
    }
}