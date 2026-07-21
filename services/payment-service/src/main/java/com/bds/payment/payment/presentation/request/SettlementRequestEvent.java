package com.bds.payment.payment.presentation.request;

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