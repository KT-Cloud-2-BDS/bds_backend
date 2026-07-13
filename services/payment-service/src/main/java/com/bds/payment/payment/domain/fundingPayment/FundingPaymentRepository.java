package com.bds.payment.payment.domain.fundingPayment;

import java.util.Optional;

public interface FundingPaymentRepository {

    FundingPayment save(FundingPayment fundingPayment);

    boolean existsByOrderId(Long orderId);

    Optional<FundingPayment> findByOrderId(Long orderId);
}
