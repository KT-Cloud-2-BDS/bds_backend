package com.bds.payment.payment.infrastructure.external.request;

import com.bds.payment.payment.presentation.request.AccountRegisterRequestDto;

public record BankAccountRequestDto(
        String bankCode,
        String accountNumber,
        String holderName
) {
    public static BankAccountRequestDto to(AccountRegisterRequestDto dto){
        return new BankAccountRequestDto(dto.bankCode(), dto.accountNumber(), dto.holderName());
    }
}
