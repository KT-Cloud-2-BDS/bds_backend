package com.bds.order.presentation.dto;

import com.bds.order.domain.order.OrderStatus;

import java.time.LocalDateTime;

public record OrderCreateResponseDto(
        Long memberId,
        String orderNo,
        Long totalBillingAmount,
        OrderStatus orderStatus,
        LocalDateTime payRequestedAt
) {
}
