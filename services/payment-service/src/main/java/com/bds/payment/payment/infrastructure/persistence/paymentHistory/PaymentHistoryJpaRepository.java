package com.bds.payment.payment.infrastructure.persistence.paymentHistory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface PaymentHistoryJpaRepository extends JpaRepository<PaymentHistoryJpaEntity, Long> {

    boolean existsByTranSeqNo(UUID tranSeqNo);

    Page<PaymentHistoryJpaEntity> findByWalletIdAndCreatedAtBetween(
            Long walletId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );
}