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

    public static final int MAX_RETRY = 3;

    private Long id;
    private Long orderId;
    private Long walletId;
    private Long productId;
    private UUID tranSeqNo;
    private Long amount;
    private PaymentType paymentType;
    private FundingPaymentStatus status;
    private Integer retryCnt;
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
                .retryCnt(0)
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

    /**
     * 결제 성공 처리
     * null(신규) 또는 FAILED(재시도) 상태에서만 호출 가능
     */
    public void markSuccess() {
        if (this.status != null && this.status != FundingPaymentStatus.FAILED) {
            throw new BusinessException(ErrorCode.FUNDING_INVALID_STATUS);
        }
        this.status = FundingPaymentStatus.SUCCESS;
    }

    /**
     * 결제 실패 처리 (retryCnt 증가)
     * null(신규) 또는 FAILED(재시도) 상태에서만 호출 가능
     */
    public void markFailed() {
        if (this.status != null && this.status != FundingPaymentStatus.FAILED) {
            throw new BusinessException(ErrorCode.FUNDING_INVALID_STATUS);
        }
        this.status = FundingPaymentStatus.FAILED;
        this.retryCnt++;
    }

    public boolean isMaxRetryExceeded() {
        return this.retryCnt >= MAX_RETRY;
    }
}