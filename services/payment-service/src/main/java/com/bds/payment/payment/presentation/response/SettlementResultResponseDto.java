package com.bds.payment.payment.presentation.response;

import java.util.List;
import java.util.UUID;

public record SettlementResultResponseDto(
        UUID batchId,
        List<SettlementResultItem> successItems,
        List<SettlementResultItem> failedItems
) {
    public record SettlementResultItem(
            Long orderId,
            boolean success,
            String message
    ) {}
}
