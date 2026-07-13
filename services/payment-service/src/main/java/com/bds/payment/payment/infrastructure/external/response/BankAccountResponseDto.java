package com.bds.payment.payment.infrastructure.external.response;

public record BankAccountResponseDto(
        String accountNumber,
        String holderName
) {
}