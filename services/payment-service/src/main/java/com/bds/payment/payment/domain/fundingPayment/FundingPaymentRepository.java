package com.bds.payment.payment.domain.fundingPayment;

import com.bds.payment.payment.domain.common.FundingPaymentStatus;

import java.util.List;
import java.util.Optional;

public interface FundingPaymentRepository {

    FundingPayment save(FundingPayment fundingPayment);

    boolean existsByOrderId(Long orderId);

    Optional<FundingPayment> findByOrderId(Long orderId);

    List<FundingPayment> findUncreditedForUpdate(Long productId, FundingPaymentStatus status);

    List<FundingPayment> saveAll(List<FundingPayment> fundingPayments);
}
