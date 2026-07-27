package com.bds.payment.payment.domain.common;

public enum CancelReason {
    INSUFFICIENT_BALANCE,     // 잔액 부족 (재시도 가능)
    MAX_RETRY_EXCEEDED,       // 재시도 3회 초과 (최종 실패)
    PAYMENT_SERVER_ERROR,     // 결제 서버 오류
    // SETTLEMENT_FAILED,     // 정산 실패 (즉시 결제 정산 시?)
    // RESERVED_FUNDING_FAILED, // 예약 펀딩 정산 실패?
}
