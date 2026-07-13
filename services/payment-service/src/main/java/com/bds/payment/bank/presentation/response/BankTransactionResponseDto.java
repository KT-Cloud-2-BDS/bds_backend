package com.bds.payment.bank.presentation.response;

import com.bds.payment.bank.domain.bankTransaction.BankTransaction;

import java.util.UUID;

public record BankTransactionResponseDto(
        UUID tranSeqNo,
        Long amount
) {
    public static BankTransactionResponseDto from(BankTransaction bankTransaction) {
        return new BankTransactionResponseDto(bankTransaction.getTranSeqNo(), bankTransaction.getAmount());
    }
}
