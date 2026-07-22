package com.bds.order.infrastructure.messaging.dto;

public record PaymentCancelledMessage(
        Long orderId,
        String cancelReason
) {
}
