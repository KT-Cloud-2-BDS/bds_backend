package com.bds.common.events.order;

import com.bds.common.events.PublishTo;

import java.util.UUID;

@PublishTo(exchange = "payment.exchange", routingKey = "payment.process.pay")
public record PaymentProcessPayEvent(
        String requestId,
        Long orderId,
        Long memberId,
        Long amount,
        String paymentType
) {
    public static PaymentProcessPayEvent of(Long orderId, Long memberId, Long amount) {
        return new PaymentProcessPayEvent(
                UUID.randomUUID().toString(),
                orderId,
                memberId,
                amount,
                "INSTANT"
        );
    }
}