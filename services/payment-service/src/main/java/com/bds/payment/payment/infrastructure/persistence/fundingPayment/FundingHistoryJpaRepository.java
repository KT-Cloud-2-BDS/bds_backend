package com.bds.payment.payment.infrastructure.persistence.fundingPayment;

import com.bds.payment.payment.domain.common.FundingPaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FundingHistoryJpaRepository extends JpaRepository<FundingPaymentJpaEntity, Long> {

    boolean existsByOrderId(Long orderId);

    Optional<FundingPaymentJpaEntity> findByOrderId(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT fp FROM FundingPaymentJpaEntity fp WHERE fp.productId = :productId AND fp.status = :status AND fp.creditedAt IS NULL")
    List<FundingPaymentJpaEntity> findUncreditedForUpdate(
            @Param("productId") Long productId,
            @Param("status") FundingPaymentStatus status
    );
}
