package com.bds.payment.payment.application.funding;

import com.bds.payment.payment.domain.common.PaymentType;
import com.bds.payment.payment.presentation.request.FundingPaymentRequestDto;
import com.bds.payment.payment.presentation.request.SettlementBatchRequestDto.SettlementItem;

public record PaymentContext(
        Long orderId,
        Long memberId,
        Long walletId,
        Long productId,
        Long amount,
        PaymentType paymentType,
        String historyMessage
) {

    public static PaymentContext forInstant(FundingPaymentRequestDto dto, Long walletId) {
        return new PaymentContext(
                dto.orderId(),
                dto.memberId(),
                walletId,
                dto.productId(),
                dto.amount(),
                PaymentType.INSTANT,
                "즉시펀딩 결제"
        );
    }

    public static PaymentContext forReserved(SettlementItem item, Long productId, Long walletId) {
        return new PaymentContext(
                item.orderId(),
                item.memberId(),
                walletId,
                productId,
                item.amount(),
                PaymentType.RESERVED,
                "예약펀딩 청구"
        );
    }

    /**
     * Handler에 전달용 (기존 dto 시그니처 호환)
     */
    public FundingPaymentRequestDto toDto() {
        return new FundingPaymentRequestDto(
                orderId,
                memberId,
                productId,
                amount,
                paymentType
        );
    }
}
