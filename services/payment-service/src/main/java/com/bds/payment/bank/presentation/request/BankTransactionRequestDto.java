package com.bds.payment.bank.presentation.request;

import java.util.UUID;

public record BankTransactionRequestDto(
        String accountNumber,
        Long amount,
        UUID tranSeqNo
) {
}
