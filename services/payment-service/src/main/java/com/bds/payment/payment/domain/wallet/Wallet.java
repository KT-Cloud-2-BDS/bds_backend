package com.bds.payment.payment.domain.wallet;

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
        if (this.balance < amount) throw new IllegalArgumentException("잔액이 부족 합니다.");
        this.balance -= amount;
    }
}
