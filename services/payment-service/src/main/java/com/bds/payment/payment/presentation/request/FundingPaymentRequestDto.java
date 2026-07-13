package com.bds.payment.payment.presentation.request;

import com.bds.payment.payment.domain.common.PaymentType;

public record FundingPaymentRequestDto(
        //TODO: Kafka를 사용함에 따라 컨트롤러 미사용으로 인지
        Long orderId,
        Long memberId,
        Long productId,
        Long amount,
        PaymentType paymentType
) {
}
