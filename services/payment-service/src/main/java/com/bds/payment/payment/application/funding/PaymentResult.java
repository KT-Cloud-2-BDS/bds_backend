package com.bds.payment.payment.application.funding;

import com.bds.payment.payment.domain.fundingPayment.FundingPayment;

public sealed interface PaymentResult {

    FundingPayment funding();

    /**
     * 결제 성공 (신규 or 재시도 성공)
     */
    record Success(FundingPayment funding) implements PaymentResult {}

    /**
     * 이미 결제된 주문 (멱등)
     */
    record AlreadyPaid(FundingPayment funding) implements PaymentResult {}

    /**
     * 유저 귀책 실패 (잔액 부족 등, 재시도 가능)
     */
    record UserFailure(FundingPayment funding) implements PaymentResult {}

    /**
     * 재시도 초과 (최종 실패)
     */
    record MaxRetryExceeded(FundingPayment funding) implements PaymentResult {}
}