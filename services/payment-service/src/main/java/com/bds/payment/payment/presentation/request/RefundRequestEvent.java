package com.bds.payment.payment.presentation.request;

import java.util.UUID;

public record RefundRequestEvent(
        UUID requestId,
        Long orderId,
        Long memberId,
        Long amount,
        String cancelReason
) {}