package com.bds.common.events.payment;

import java.util.List;
import java.util.UUID;

public record SettlementRequestEvent(
        UUID batchId,
        String type,
        Long creatorMemberId,
        Long productId,
        List<Item> items
) {
    public record Item(
            Long orderId,
            Long memberId,
            Long amount
    ) {}
}