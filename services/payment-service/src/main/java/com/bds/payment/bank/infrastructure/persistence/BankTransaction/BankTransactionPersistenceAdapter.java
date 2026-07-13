package com.bds.payment.bank.infrastructure.persistence.BankTransaction;

import com.bds.payment.bank.domain.bankTransaction.BankTransaction;
import com.bds.payment.bank.domain.bankTransaction.BankTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BankTransactionPersistenceAdapter implements BankTransactionRepository {

    private final BankTransactionJpaRepository jpaRepository;
    private final BankTransactionMapper mapper;

    @Override
    public BankTransaction save(BankTransaction bankTransaction) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(bankTransaction)));
    }

    @Override
    public boolean existsByTranSeqNo(UUID tranSeqNo) {
        return jpaRepository.existsByTranSeqNo(tranSeqNo);
    }
}
