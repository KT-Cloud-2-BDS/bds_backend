package com.bds.payment.payment.presentation.request;

public record AccountRegisterRequestDto(
        String bankCode,
        String accountNumber,
        String holderName
) {
}