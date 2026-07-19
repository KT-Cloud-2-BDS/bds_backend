package com.bds.common.events.payment;

import java.util.UUID;

public record RefundRequestEvent(
        UUID requestId,
        Long orderId,
        Long memberId,
        Long amount,
        String cancelReason
) {}