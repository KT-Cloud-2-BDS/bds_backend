package com.bds.payment.payment.infrastructure.persistence.fundingPayment;

import com.bds.payment.payment.domain.fundingPayment.FundingPayment;
import com.bds.payment.payment.domain.fundingPayment.FundingPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FundingPaymentPersistenceAdapter implements FundingPaymentRepository {

    private final FundingHistoryJpaRepository jpaRepository;
    private final FundingPaymentMapper mapper;

    @Override
    public FundingPayment save(FundingPayment fundingPayment) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(fundingPayment)));
    }

    @Override
    public boolean existsByOrderId(Long orderId) {
        return jpaRepository.existsByOrderId(orderId);
    }

    @Override
    public Optional<FundingPayment> findByOrderId(Long orderId) {
        return jpaRepository.findByOrderId(orderId).map(mapper::toDomain);
    }
}
