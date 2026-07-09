package com.bds.payment.payment.domain.account;

import com.bds.payment.payment.presentation.request.AccountRegisterRequestDto;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class Account {

    private Long walletId;
    private String bankCode;
    private String accountNumber;
    private String holderName;
    private Boolean isVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Account create(Long walletId, AccountRegisterRequestDto dto) {
        LocalDateTime now = LocalDateTime.now();
        return Account.builder()
                .walletId(walletId)
                .bankCode(dto.bankCode())
                .accountNumber(dto.accountNumber())
                .holderName(dto.holderName())
                .isVerified(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void updateAccount(AccountRegisterRequestDto dto) {
        this.accountNumber = dto.accountNumber();
        this.bankCode = dto.bankCode();
        this.holderName = dto.holderName();
        this.isVerified = false;
    }
}
