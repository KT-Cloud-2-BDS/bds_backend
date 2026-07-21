package com.bds.payment.payment.domain.common;

/**
 * 지갑(Wallet) 기준 거래 유형
 * - DEPOSIT: 지갑에 금액이 증가하는 거래 (충전, 환불 등)
 * - WITHDRAWAL: 지갑에서 금액이 감소하는 거래 (출금, 결제 등)
 */
public enum TransactionType {
    DEPOSIT, WITHDRAWAL
}
