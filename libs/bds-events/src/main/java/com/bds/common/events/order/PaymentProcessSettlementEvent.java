package com.bds.common.events.order;

import org.springframework.modulith.events.Externalized;

import java.util.List;
import java.util.UUID;

@Externalized("payment.exchange::payment.process.settlement")
public record PaymentProcessSettlementEvent(
        String batchId,
        String type,
        Long creatorMemberId,
        List<SettlementItem> items
) {
    public static PaymentProcessSettlementEvent of(String type, Long creatorMemberId, List<SettlementItem> items) {
        return new PaymentProcessSettlementEvent(
                UUID.randomUUID().toString(),
                type,
                creatorMemberId,
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