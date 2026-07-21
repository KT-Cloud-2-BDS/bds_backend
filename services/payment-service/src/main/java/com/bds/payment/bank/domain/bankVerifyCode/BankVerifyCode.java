package com.bds.payment.bank.domain.bankVerifyCode;

import com.bds.payment.bank.presentation.request.BankAccountRequestDto;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BankVerifyCode {

    private Long id;
    private String bankCode;
    private String accountNumber;
    private String holderName;
    private String verifyCode;
    private LocalDateTime createdAt;

    public static BankVerifyCode create(BankAccountRequestDto dto, String code) {
        LocalDateTime now = LocalDateTime.now();
        return BankVerifyCode.builder()
                .bankCode(dto.bankCode())
                .accountNumber(dto.accountNumber())
                .holderName(dto.holderName())
                .verifyCode(code)
                .createdAt(now)
                .build();
    }
}
