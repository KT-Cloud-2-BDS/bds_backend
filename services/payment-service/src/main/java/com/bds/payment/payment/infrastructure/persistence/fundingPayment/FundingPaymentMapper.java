package com.bds.payment.payment.infrastructure.persistence.fundingPayment;

import com.bds.payment.payment.domain.fundingPayment.FundingPayment;
import org.springframework.stereotype.Component;

@Component
public class FundingPaymentMapper {

    FundingPayment toDomain(FundingPaymentJpaEntity entity) {
        return FundingPayment.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .walletId(entity.getWalletId())
                .productId(entity.getProductId())
                .tranSeqNo(entity.getTranSeqNo())
                .amount(entity.getAmount())
                .paymentType(entity.getPaymentType())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    FundingPaymentJpaEntity toEntity(FundingPayment domain) {
        return FundingPaymentJpaEntity.builder()
                .id(domain.getId())
                .orderId(domain.getOrderId())
                .walletId(domain.getWalletId())
                .productId(domain.getProductId())
                .tranSeqNo(domain.getTranSeqNo())
                .amount(domain.getAmount())
                .paymentType(domain.getPaymentType())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
