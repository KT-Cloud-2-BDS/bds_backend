package com.bds.payment.bank.presentation.request;

public record BankVerifyRequestDto(
        String accountNumber,
        String code
) {
}
