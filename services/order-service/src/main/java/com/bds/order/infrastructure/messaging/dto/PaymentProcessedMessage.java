package com.bds.order.infrastructure.messaging.dto;

import java.util.List;

public record PaymentProcessedMessage(
        List<Long> orderIds,
        ResultType type
) {
    public enum ResultType {
        CONFIRMED, REFUNDED
    }
}
