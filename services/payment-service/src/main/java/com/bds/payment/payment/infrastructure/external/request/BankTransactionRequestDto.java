package com.bds.payment.payment.infrastructure.external.request;

import java.util.UUID;

public record BankTransactionRequestDto(
        String accountNumber,
        Long amount,
        UUID tranSeqNo
) {
    public static BankTransactionRequestDto create(
            String accountNumber,
            Long amount,
            UUID tranSeqNo
    ) {
        return new BankTransactionRequestDto(
               accountNumber,
               amount,
               tranSeqNo
        );
    }
}
