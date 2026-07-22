package com.bds.common.events.order;

import org.springframework.modulith.events.Externalized;

import java.util.List;
import java.util.UUID;

@Externalized("order.exchange::order.settle.requested")
public record OrderProcessSettlementEvent(
        UUID batchId,
        String type,
        Long creatorMemberId,
        Long fundingId,
        List<SettlementItem> items
) {
    public static OrderProcessSettlementEvent of(String type, Long creatorMemberId, Long fundingId, List<SettlementItem> items) {
        return new OrderProcessSettlementEvent(
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