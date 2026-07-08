package com.bds.payment.payment.infrastructure.persistence.fundingPayment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FundingHistoryJpaRepository extends JpaRepository<FundingPaymentJpaEntity, Long> {

    boolean existsByOrderId(Long orderId);

    Optional<FundingPaymentJpaEntity> findByOrderId(Long orderId);
}
