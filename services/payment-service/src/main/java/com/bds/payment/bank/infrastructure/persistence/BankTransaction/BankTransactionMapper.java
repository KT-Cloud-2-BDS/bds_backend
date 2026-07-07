package com.bds.payment.bank.infrastructure.persistence.BankTransaction;

import com.bds.payment.bank.domain.bankTransaction.BankTransaction;
import org.springframework.stereotype.Component;

@Component
public class BankTransactionMapper {

    BankTransaction toDomain(BankTransactionJpaEntity entity) {
        return BankTransaction.builder()
                .id(entity.getId())
                .tranSeqNo(entity.getTranSeqNo())
                .accountNumber(entity.getAccountNumber())
                .type(entity.getType())
                .amount(entity.getAmount())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    BankTransactionJpaEntity toEntity(BankTransaction domain) {
        return BankTransactionJpaEntity.builder()
                .id(domain.getId())
                .tranSeqNo(domain.getTranSeqNo())
                .accountNumber(domain.getAccountNumber())
                .type(domain.getType())
                .amount(domain.getAmount())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
