package com.bds.payment.payment.domain.common;

public enum SettlementType {
    SETTLEMENT_CONFIRMED,          // 정산확정 (즉시펀딩)
    RESERVED_FUNDING_CONFIRMED,    // 예약펀딩확정
    FUNDING_FAILED_REFUND          // 펀딩실패 환불
}
