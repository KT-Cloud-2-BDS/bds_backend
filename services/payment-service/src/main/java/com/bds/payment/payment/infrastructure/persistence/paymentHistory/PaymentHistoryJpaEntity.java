package com.bds.payment.payment.infrastructure.persistence.paymentHistory;

import com.bds.payment.payment.domain.common.PaymentHistoryStatus;
import com.bds.payment.payment.domain.common.TransactionReason;
import com.bds.payment.payment.domain.common.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Builder
@Table(name = "payment_history")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentHistoryJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long walletId;

    private Long fundingPaymentId;

    @Column(nullable = false)
    private UUID tranSeqNo;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType type; // DEPOSIT | WITHDRAWAL

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionReason reason; // CHARGE | WITHDRAW | FUNDING_PAYMENT | FUNDING_REFUND | SETTLEMENT

    @Column(length = 50)
    private String message;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private Long balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentHistoryStatus status; // SUCCESS | FAILED | COMPENSATED

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}