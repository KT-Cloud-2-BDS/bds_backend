package com.bds.common.events.order;

import org.springframework.modulith.events.Externalized;

import java.util.UUID;

@Externalized("payment.exchange::payment.process.pay")
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