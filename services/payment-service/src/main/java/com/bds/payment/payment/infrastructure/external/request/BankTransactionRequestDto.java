package com.bds.payment.payment.infrastructure.external.request;

public record BankTransactionRequestDto(
        String accountNumber,
        Long amount,
        String tranSeqNo
) {
}
