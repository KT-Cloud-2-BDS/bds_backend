package com.bds.common.events.order;

import org.springframework.modulith.events.Externalized;

import java.util.UUID;

@Externalized("payment.exchange::payment.process.refund")
public record PaymentProcessRefundEvent(
        UUID requestId,
        Long orderId,
        Long memberId,
        Long fundingId,
        Long amount,
        String cancelReason
) {
    public static PaymentProcessRefundEvent of(Long orderId, Long memberId, Long fundingId, Long amount, String cancelReason) {
        return new PaymentProcessRefundEvent(
                UUID.randomUUID(),
                orderId,
                memberId,
                fundingId,
                amount,
                cancelReason
        );
    }
}