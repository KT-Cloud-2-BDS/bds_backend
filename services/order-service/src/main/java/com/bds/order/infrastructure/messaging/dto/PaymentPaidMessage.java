package com.bds.order.infrastructure.messaging.dto;

public record PaymentPaidMessage (
        Long orderId
) {
}