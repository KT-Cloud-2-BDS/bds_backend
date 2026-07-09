package com.bds.order.presentation.dto;

import java.time.LocalDateTime;

public record OrderCreateResponseDto(
        Long memberId,
        String orderNo,
        Long totalBillingAmount,
        String paymentStatus,
        LocalDateTime paidAt
) {
}
