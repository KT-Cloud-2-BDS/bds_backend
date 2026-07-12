package com.bds.payment.payment.presentation.response;

import com.bds.payment.payment.domain.common.PaymentHistoryStatus;
import com.bds.payment.payment.domain.common.TransactionReason;
import com.bds.payment.payment.domain.paymentHistory.PaymentHistory;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentHistoryResponseDto(
        UUID tranSeqNo,
        TransactionReason type,
        String message,
        Long amount,
        Long balanceAfter,
        PaymentHistoryStatus status,
        LocalDateTime createdAt
) {
    public static PaymentHistoryResponseDto from(PaymentHistory history) {
        return new PaymentHistoryResponseDto(
                history.getTranSeqNo(),
                history.getReason(),
                history.getMessage(),
                history.getAmount(),
                history.getBalanceAfter(),
                history.getStatus(),
                history.getCreatedAt()
        );
    }

}
