package com.bds.payment.payment.domain.paymentHistory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

public interface PaymentHistoryRepository {

    PaymentHistory save(PaymentHistory paymentHistory);

    boolean existsByTranSeqNo(UUID tranSeqNo);

    Page<PaymentHistory> findByWalletIdAndCreatedAtBetween(Long walletId, LocalDateTime from, LocalDateTime to, Pageable pageable);
}
