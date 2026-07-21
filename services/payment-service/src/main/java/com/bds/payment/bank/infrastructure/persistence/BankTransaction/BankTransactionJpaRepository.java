package com.bds.payment.bank.infrastructure.persistence.BankTransaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BankTransactionJpaRepository extends JpaRepository<BankTransactionJpaEntity, Long> {
    boolean existsByTranSeqNo(UUID TranSeqNo);
}
