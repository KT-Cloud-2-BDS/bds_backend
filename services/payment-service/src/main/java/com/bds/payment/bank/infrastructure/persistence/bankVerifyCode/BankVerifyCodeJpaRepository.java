package com.bds.payment.bank.infrastructure.persistence.bankVerifyCode;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface BankVerifyCodeJpaRepository extends JpaRepository<BankVerifyCodeJpaEntity, Long> {

    Optional<BankVerifyCodeJpaEntity> findByAccountNumber(String accountNumber);
}
