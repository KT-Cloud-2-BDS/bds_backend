package com.bds.payment.bank.presentation.request;

public record BankAccountRequestDto(
        String bankCode,
        String accountNumber,
        String holderName
) {
}
