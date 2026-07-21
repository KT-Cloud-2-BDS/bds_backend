package com.bds.payment.payment.domain.wallet;

import com.bds.payment.payment.global.exception.BusinessException;
import com.bds.payment.payment.global.exception.ErrorCode;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class Wallet {

    private Long id;
    private Long memberId;
    private Long balance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Wallet create(Long memberId) {
        LocalDateTime now = LocalDateTime.now();
        return Wallet.builder()
                .memberId(memberId)
                .balance(0L)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void charge(Long amount) {
        this.balance += amount;
    }

    public void withdraw(Long amount) {
        if (this.balance < amount) throw new BusinessException(ErrorCode.WALLET_INSUFFICIENT_BALANCE);
        this.balance -= amount;
    }
}
