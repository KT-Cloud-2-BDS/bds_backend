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
        this.balance += amount; //TODO: amount 검증 필수
    }

    public void withdraw(Long amount) {
        this.balance -= amount; //TODO: amount 검증 필수 및 출금이 불가능한 경우(ex. 잔액부족) 검증 필요
    }
}
