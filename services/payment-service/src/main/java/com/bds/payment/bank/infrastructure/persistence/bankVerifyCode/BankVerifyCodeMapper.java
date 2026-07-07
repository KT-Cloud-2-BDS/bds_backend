package com.bds.payment.bank.infrastructure.persistence.bankVerifyCode;

import com.bds.payment.bank.domain.bankVerifyCode.BankVerifyCode;
import org.springframework.stereotype.Component;

@Component
public class BankVerifyCodeMapper {

    BankVerifyCode toDomain(BankVerifyCodeJpaEntity entity) {
        return BankVerifyCode.builder()
                .id(entity.getId())
                .bankCode(entity.getBankCode())
                .accountNumber(entity.getAccountNumber())
                .holderName(entity.getHolderName())
                .verifyCode(entity.getVerifyCode())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    BankVerifyCodeJpaEntity toEntity(BankVerifyCode domain) {
        return BankVerifyCodeJpaEntity.builder()
                .id(domain.getId())
                .bankCode(domain.getBankCode())
                .accountNumber(domain.getAccountNumber())
                .holderName(domain.getHolderName())
                .verifyCode(domain.getVerifyCode())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
