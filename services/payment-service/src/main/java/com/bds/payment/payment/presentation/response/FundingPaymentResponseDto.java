package com.bds.payment.payment.presentation.response;

import com.bds.payment.payment.domain.common.PaymentType;
import com.bds.payment.payment.domain.fundingPayment.FundingPayment;

import java.util.UUID;

public record FundingPaymentResponseDto(
        Long orderId,
        Long productId,
        Long memberId,
        Long amount,
        PaymentType paymentType,
        UUID tranSeqNo
) {
    public static FundingPaymentResponseDto from(FundingPayment fundingPayment, Long memberId) {
        return new FundingPaymentResponseDto(
                fundingPayment.getOrderId(),
                fundingPayment.getProductId(),
                memberId,
                fundingPayment.getAmount(),
                fundingPayment.getPaymentType(),
                fundingPayment.getTranSeqNo()
        );
    }
}