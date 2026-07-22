package com.bds.common.events.order;

import org.springframework.modulith.events.Externalized;

import java.util.UUID;

@Externalized("order.exchange::order.refund.requested")
public record OrderProcessRefundEvent(
        UUID requestId,
        Long orderId,
        Long memberId,
        Long fundingId,
        Long amount,
        String cancelReason
) {
    public static OrderProcessRefundEvent of(Long orderId, Long memberId, Long fundingId, Long amount, String cancelReason) {
        return new OrderProcessRefundEvent(
                UUID.randomUUID(),
                orderId,
                memberId,
                fundingId,
                amount,
                cancelReason
        );
    }
}