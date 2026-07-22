package com.bds.payment.payment.presentation.request;

import com.bds.payment.payment.domain.common.PaymentType;

public record FundingPaymentRequestDto(
        Long orderId,
        Long memberId,
        Long productId,
        Long amount,
        PaymentType paymentType
) {
}
