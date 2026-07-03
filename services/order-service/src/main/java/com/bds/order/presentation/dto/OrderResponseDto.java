package com.bds.order.presentation.dto;

import com.bds.order.domain.order.Order;

import java.time.LocalDateTime;

public record OrderResponseDto(
        String orderNo,
        OrderStatus orderStatus,
        LocalDateTime fundingDate,
        String title,
        String hostName,
        boolean isEnded,
        Long billingAmount,
        String paymentStatus,
        LocalDateTime paidAt,
        boolean isFundingSucceeded
) {
    public static OrderResponseDto from(Order order) {
        return new OrderResponseDto(
                order.getOrderNo(),
                order.getStatus(),
                order.getCreatedAt(),
                null,
                null,
                false,
                order.getAmount(),
                null,
                null,
                false
        );
    }
}
