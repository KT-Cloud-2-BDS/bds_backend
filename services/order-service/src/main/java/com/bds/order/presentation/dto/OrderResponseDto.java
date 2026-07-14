package com.bds.order.presentation.dto;

import com.bds.order.domain.order.OrderStatus;
import com.bds.order.infrastructure.order.OrderListProjection;

import java.time.LocalDateTime;

public record OrderResponseDto(
        Long orderId,
        String orderNo,
        OrderStatus orderStatus,
        LocalDateTime fundingDate,
        String title,
        Long hostId,
        boolean isEnded,
        Long billingAmount,
        LocalDateTime updatedAt,
        boolean isFundingSucceeded
) {
    public static OrderResponseDto from(OrderListProjection order) {
        return new OrderResponseDto(
                order.orderId(),
                order.orderNo(),
                order.status(),
                order.createdAt(),
                order.fundingTitle(),
                order.hostId(),
                LocalDateTime.now().isAfter(order.holdTo()),
                order.totalRewardAmount() + order.totalShippingCharge(),
                order.updatedAt(),
                order.isSuccess()
        );
    }
}
