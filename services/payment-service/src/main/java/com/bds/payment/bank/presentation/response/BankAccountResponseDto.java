package com.bds.payment.bank.presentation.response;

import com.bds.payment.bank.domain.bankVerifyCode.BankVerifyCode;

public record BankAccountResponseDto(
        String accountNumber,
        String holderName
) {
    public static BankAccountResponseDto from(BankVerifyCode bankVerifyCode) {
        return new BankAccountResponseDto(bankVerifyCode.getAccountNumber(), bankVerifyCode.getHolderName());
    }
}
