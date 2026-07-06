package com.bds.payment.payment.infrastructure.persistence.wallet;

import com.bds.payment.payment.domain.wallet.Wallet;
import org.springframework.stereotype.Component;

@Component
public class WalletMapper {

    Wallet toDomain(WalletJpaEntity entity) {
        return Wallet.builder()
                .id(entity.getId())
                .memberId(entity.getMemberId())
                .balance(entity.getBalance())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    WalletJpaEntity toJpaEntity(Wallet domain) {
        return WalletJpaEntity.builder()
                .id(domain.getId())
                .memberId(domain.getMemberId())
                .balance(domain.getBalance())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
