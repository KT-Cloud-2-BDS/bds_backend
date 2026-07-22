package com.bds.payment.payment.domain.fundingPayment;

import com.bds.payment.payment.domain.common.FundingPaymentStatus;
import com.bds.payment.payment.domain.common.PaymentType;
import com.bds.payment.payment.global.exception.BusinessException;
import com.bds.payment.payment.global.exception.ErrorCode;
import com.bds.payment.payment.presentation.request.FundingPaymentRequestDto;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class FundingPayment {

    private Long id;
    private Long orderId;
    private Long walletId;
    private Long productId;
    private UUID tranSeqNo;
    private Long amount;
    private PaymentType paymentType;
    private FundingPaymentStatus status;
    private LocalDateTime creditedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FundingPayment create(FundingPaymentRequestDto dto, Long walletId, UUID tranSeqNo) {
        return FundingPayment.builder()
                .orderId(dto.orderId())
                .walletId(walletId)
                .productId(dto.productId())
                .tranSeqNo(tranSeqNo)
                .amount(dto.amount())
                .paymentType(dto.paymentType())
                .status(FundingPaymentStatus.SUCCESS)
                .build();

    }

    public void refund() {
        this.status = FundingPaymentStatus.REFUNDED;
    }

    public void confirm() {
        if (this.status != FundingPaymentStatus.SUCCESS) {
            throw new BusinessException(ErrorCode.FUNDING_INVALID_STATUS);
        }
        this.status = FundingPaymentStatus.CONFIRMED;
    }

    public void confirmReserved() {
        if (this.paymentType != PaymentType.RESERVED) {
            throw new BusinessException(ErrorCode.FUNDING_INVALID_STATUS);
        }
        if (this.status != FundingPaymentStatus.RESERVED) {
            throw new BusinessException(ErrorCode.FUNDING_INVALID_STATUS);
        }
        this.status = FundingPaymentStatus.CONFIRMED;
    }

    public void markCredited(LocalDateTime now) {
        if (this.status != FundingPaymentStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.FUNDING_INVALID_STATUS);
        }
        if (this.creditedAt != null) {
            return;  // 이미 크레딧됨 (멱등)
        }
        this.creditedAt = now;
    }
}
