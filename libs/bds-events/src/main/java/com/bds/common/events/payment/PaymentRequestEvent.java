package com.bds.common.events.payment;

import java.util.UUID;

public record PaymentRequestEvent(
        UUID requestId,
        Long orderId,
        Long memberId,
        Long productId,
        Long amount,
        String paymentType
) {}