package com.bds.order.infrastructure.order;

import com.bds.order.domain.order.OrderStatus;

import java.time.LocalDateTime;

public record OrderListProjection(
        Long orderId,
        String orderNo,
        OrderStatus status,
        Long totalRewardAmount,
        Long totalShippingCharge,
        LocalDateTime createdAt,
        String fundingTitle,
        Long hostId,
        LocalDateTime holdTo,
        Boolean isSuccess
) {
}