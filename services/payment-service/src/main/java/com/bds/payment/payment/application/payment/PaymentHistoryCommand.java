package com.bds.payment.payment.application.payment;

import com.bds.payment.payment.domain.common.PaymentHistoryStatus;
import com.bds.payment.payment.domain.common.TransactionReason;
import com.bds.payment.payment.domain.common.TransactionType;
import com.bds.payment.payment.domain.wallet.Wallet;

import java.util.UUID;

public record PaymentHistoryCommand(
        Long walletId,
        Long fundingPaymentId,  // nullable
        UUID tranSeqNo,
        TransactionType type,
        TransactionReason reason,
        String message,         // nullable
        Long amount,
        Long balanceAfter,
        PaymentHistoryStatus status
) {
    // 충전/출금용 (fundingPaymentId, message 없음)
    public static PaymentHistoryCommand of(
            Wallet wallet,
            UUID tranSeqNo,
            TransactionType type,
            TransactionReason reason,
            Long amount,
            PaymentHistoryStatus status
    ) {
        return new PaymentHistoryCommand(
                wallet.getId(),
                null,
                tranSeqNo,
                type,
                reason,
                null,
                amount,
                wallet.getBalance(),
                status
        );
    }

    // 펀딩용 (fundingPaymentId 있음)
    public static PaymentHistoryCommand ofFunding(
            Long walletId,
            Long fundingPaymentId,
            UUID tranSeqNo,
            TransactionType type,
            TransactionReason reason,
            String message,
            Long amount,
            Long balanceAfter,
            PaymentHistoryStatus status
    ) {
        return new PaymentHistoryCommand(
                walletId,
                fundingPaymentId,
                tranSeqNo,
                type,
                reason,
                message,
                amount,
                balanceAfter,
                status
        );
    }
}