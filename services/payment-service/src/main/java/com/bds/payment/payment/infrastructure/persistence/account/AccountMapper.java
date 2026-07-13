package com.bds.payment.payment.infrastructure.persistence.account;

import com.bds.payment.payment.domain.account.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    Account toDomain(AccountJpaEntity entity) {
        return Account.builder()
                .walletId(entity.getWalletId())
                .bankCode(entity.getBankCode())
                .accountNumber(entity.getAccountNumber())
                .holderName(entity.getHolderName())
                .isVerified(entity.getIsVerified())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    AccountJpaEntity toEntity(Account domain) {
        return AccountJpaEntity.builder()
                .walletId(domain.getWalletId())
                .bankCode(domain.getBankCode())
                .accountNumber(domain.getAccountNumber())
                .holderName(domain.getHolderName())
                .isVerified(domain.getIsVerified())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}