package com.bds.payment.payment.domain.paymentHistory;

import com.bds.payment.payment.application.payment.PaymentHistoryCommand;
import com.bds.payment.payment.domain.common.PaymentHistoryStatus;
import com.bds.payment.payment.domain.common.TransactionReason;
import com.bds.payment.payment.domain.common.TransactionType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class PaymentHistory {

    private Long id;
    private Long walletId;
    private Long fundingPaymentId;
    private UUID tranSeqNo;
    private TransactionType type; // DEPOSIT | WITHDRAWAL
    private TransactionReason reason; // CHARGE | WITHDRAW | FUNDING_PAYMENT | FUNDING_REFUND | SETTLEMENT
    private String message;
    private Long amount;
    private Long balanceAfter;
    private PaymentHistoryStatus status;
    private LocalDateTime createdAt;

    public static PaymentHistory create(PaymentHistoryCommand command) {
        return PaymentHistory.builder()
                .walletId(command.walletId())
                .fundingPaymentId(command.fundingPaymentId())
                .tranSeqNo(command.tranSeqNo())
                .type(command.type())
                .reason(command.reason())
                .message(command.message())
                .amount(command.amount())
                .balanceAfter(command.balanceAfter())
                .status(command.status())
                .build();
    }
}
