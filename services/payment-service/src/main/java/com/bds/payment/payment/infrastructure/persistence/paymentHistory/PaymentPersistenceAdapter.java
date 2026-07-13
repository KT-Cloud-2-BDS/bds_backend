package com.bds.payment.payment.infrastructure.persistence.paymentHistory;

import com.bds.payment.payment.domain.paymentHistory.PaymentHistory;
import com.bds.payment.payment.domain.paymentHistory.PaymentHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentPersistenceAdapter implements PaymentHistoryRepository {

    private final PaymentHistoryJpaRepository jpaRepository;
    private final PaymentHistoryMapper mapper;

    @Override
    public PaymentHistory save(PaymentHistory paymentHistory) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(paymentHistory)));
    }

    @Override
    public boolean existsByTranSeqNo(UUID tranSeqNo) {
        return jpaRepository.existsByTranSeqNo(tranSeqNo);
    }

    @Override
    public Page<PaymentHistory> findByWalletIdAndCreatedAtBetween(Long walletId, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return jpaRepository.findByWalletIdAndCreatedAtBetween(walletId, from, to, pageable).map(mapper::toDomain);
    }
}
