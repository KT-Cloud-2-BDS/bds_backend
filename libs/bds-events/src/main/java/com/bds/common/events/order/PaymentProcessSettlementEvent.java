package com.bds.common.events.order;

import org.springframework.modulith.events.Externalized;

import java.util.List;
import java.util.UUID;

@Externalized("payment.exchange::payment.process.settlement")
public record PaymentProcessSettlementEvent(
        UUID batchId,
        String type,
        Long creatorMemberId,
        Long fundingId,
        List<SettlementItem> items
) {
    public static PaymentProcessSettlementEvent of(String type, Long creatorMemberId, Long fundingId, List<SettlementItem> items) {
        return new PaymentProcessSettlementEvent(
                UUID.randomUUID(),
                type,
                creatorMemberId,
                fundingId,
                items
        );
    }

    public record SettlementItem(
            Long orderId,
            Long memberId,
            Long amount
    ) {
    }
}