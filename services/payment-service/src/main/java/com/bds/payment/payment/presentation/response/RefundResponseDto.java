package com.bds.payment.payment.presentation.response;

public record RefundResponseDto(
        Long orderId,
        Long refundAmount,
        Long balance
) {
}
