package com.bds.payment.payment.infrastructure.external.request;

import com.bds.payment.payment.presentation.request.AccountVerifyRequestDto;

public record BankVerifyRequestDto(
        String accountNumber,
        String code
) {
    public static BankVerifyRequestDto create(String accountNumber, AccountVerifyRequestDto dto) {
        return new BankVerifyRequestDto(accountNumber, dto.code());
    }
}
