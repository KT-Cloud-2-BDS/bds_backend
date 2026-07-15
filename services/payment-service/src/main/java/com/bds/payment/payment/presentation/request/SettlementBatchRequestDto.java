package com.bds.payment.payment.presentation.request;

import com.bds.payment.payment.domain.common.SettlementType;

import java.util.List;
import java.util.UUID;

public record SettlementBatchRequestDto(
        UUID batchId,
        SettlementType type,
        Long creatorMemberId,   // SETTLEMENT_CONFIRMED, RESERVED_FUNDING_CONFIRMED만 필수
        List<SettlementItem> items
) {
    public record SettlementItem(
            Long orderId,
            Long memberId,      // RESERVED_FUNDING_CONFIRMED, FUNDING_FAILED_REFUND만 필수
            Long amount
    ) {}
}
