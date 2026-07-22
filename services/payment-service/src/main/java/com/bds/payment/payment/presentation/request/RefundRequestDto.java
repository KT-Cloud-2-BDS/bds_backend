package com.bds.payment.payment.presentation.request;

import java.util.UUID;

public record RefundRequestDto (
        UUID requestId,       // 멱등 처리 키
        Long orderId,
        Long memberId,
        Long productId,
        Long amount,
        String cancelReason   // 취소 사유 (UPPER_SNAKE_CASE)
) {}
