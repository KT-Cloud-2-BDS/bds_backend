package com.bds.payment.payment.infrastructure.persistence.paymentHistory;

import com.bds.payment.payment.domain.paymentHistory.PaymentHistory;
import org.springframework.stereotype.Component;

@Component
public class PaymentHistoryMapper {

    PaymentHistory toDomain(PaymentHistoryJpaEntity entity) {
        return PaymentHistory.builder()
                .id(entity.getId())
                .walletId(entity.getWalletId())
                .fundingPaymentId(entity.getFundingPaymentId())
                .tranSeqNo(entity.getTranSeqNo())
                .type(entity.getType())
                .reason(entity.getReason())
                .message(entity.getMessage())
                .amount(entity.getAmount())
                .balanceAfter(entity.getBalanceAfter())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    PaymentHistoryJpaEntity toEntity(PaymentHistory domain) {
        return PaymentHistoryJpaEntity.builder()
                .id(domain.getId())
                .walletId(domain.getWalletId())
                .fundingPaymentId(domain.getFundingPaymentId())
                .tranSeqNo(domain.getTranSeqNo())
                .type(domain.getType())
                .reason(domain.getReason())
                .message(domain.getMessage())
                .amount(domain.getAmount())
                .balanceAfter(domain.getBalanceAfter())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
