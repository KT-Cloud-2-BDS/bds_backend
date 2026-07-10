package com.bds.order.presentation.dto;

import com.bds.order.domain.order.OrderStatus;

import java.time.LocalDateTime;

public record OrderCancelResponseDto(
        String orderNo,
        OrderStatus status,
        LocalDateTime cancelledAt,
        String refundStatus
) {
}
