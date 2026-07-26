package com.bds.payment.payment.application.funding;

import com.bds.payment.payment.application.wallet.WalletService;
import com.bds.payment.payment.domain.common.CancelReason;
import com.bds.payment.payment.domain.fundingPayment.FundingPayment;
import com.bds.payment.payment.domain.fundingPayment.FundingPaymentRepository;
import com.bds.payment.payment.global.exception.BusinessException;
import com.bds.payment.payment.global.exception.ErrorCode;
import com.bds.payment.payment.presentation.request.FundingPaymentRequestDto;
import com.bds.payment.payment.presentation.request.RefundRequestDto;
import com.bds.payment.payment.presentation.request.SettlementBatchRequestDto;
import com.bds.payment.payment.presentation.request.SettlementBatchRequestDto.SettlementItem;
import com.bds.payment.payment.presentation.response.FundingPaymentResponseDto;
import com.bds.payment.payment.presentation.response.SettlementResultResponseDto;
import com.bds.payment.payment.presentation.response.SettlementResultResponseDto.SettlementResultItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FundingService {

    private final FundingPaymentProcessor paymentProcessor;
    private final WalletService walletService;
    private final FundingSettlementProcessor settlementProcessor;
    private final FundingEventPublisher eventPublisher;
    private final FundingPaymentRepository fundingPaymentRepository;
    private final FundingFailureHandler failureHandler;

    public FundingPaymentResponseDto funding(FundingPaymentRequestDto dto) {
        Long walletId = walletService.getWalletId(dto.memberId());
        PaymentContext ctx = PaymentContext.forInstant(dto, walletId);

        try {
            PaymentResult result = paymentProcessor.process(ctx);

            if (result instanceof PaymentResult.Success) {  // ← 명시적
                eventPublisher.publishOrderPaid(dto.orderId());
            }

            return FundingPaymentResponseDto.from(result.funding(), dto.memberId());

        } catch (BusinessException e) {
            if (isUserFault(e)) {
                // Processor 트랜잭션 롤백된 후 실행 → 락 해제됨
                failureHandler.handleUserFailure(ctx, e);

                // 실패 응답 (funding 도메인 상태는 Handler 안에서 저장됨)
                FundingPayment failed = fundingPaymentRepository.findByOrderId(dto.orderId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.FUNDING_NOT_FOUND));
                return FundingPaymentResponseDto.from(failed, dto.memberId());
            }
            failureHandler.handleSystemError(dto.orderId());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during funding. orderId={}", dto.orderId(), e);
            failureHandler.handleSystemError(dto.orderId());
            throw new BusinessException(ErrorCode.PAYMENT_SERVER_ERROR);
        }
    }

    public void refund(RefundRequestDto dto) {
        settlementProcessor.processRefundItem(dto.orderId(), dto.memberId(), dto.cancelReason());

        // 환불 완료 이벤트 발행
        eventPublisher.publishOrderProcessRefunded(List.of(dto.orderId()));
    }

    public SettlementResultResponseDto confirmSettlement(SettlementBatchRequestDto dto) {
        List<SettlementResultItem> successItems = new ArrayList<>();
        List<SettlementResultItem> failedItems = new ArrayList<>();
        List<Long> confirmedOrderIds = new ArrayList<>();

        for (SettlementItem item : dto.items()) {
            try {
                long amount = settlementProcessor.processSettlementItem(item);

                if (amount == 0L) {
                    successItems.add(new SettlementResultItem(item.orderId(), true, "ALREADY_CONFIRMED"));
                } else {
                    successItems.add(new SettlementResultItem(item.orderId(), true, "SUCCESS"));
                }
                confirmedOrderIds.add(item.orderId());
            } catch (Exception e) {
                log.error("Settlement failed. orderId={}", item.orderId(), e);
                failedItems.add(new SettlementResultItem(item.orderId(), false, e.getMessage()));
                eventPublisher.publishOrderCancelled(item.orderId(), CancelReason.PAYMENT_SERVER_ERROR.name());
            }
        }

        try {
            settlementProcessor.creditCreatorForBatch(dto.creatorMemberId(), dto.productId(), "정산 확정 (즉시펀딩)");
        } catch (Exception e) {
            log.error("Creator credit failed. batchId={}, creatorId={}, productId={}", dto.batchId(), dto.creatorMemberId(), dto.productId(), e);
            throw e;
        }

        // 정산 확정 이벤트 발행 (성공 건들)
        if (!confirmedOrderIds.isEmpty()) {
            eventPublisher.publishOrderProcessConfirmed(confirmedOrderIds);
        }

        return new SettlementResultResponseDto(dto.batchId(), successItems, failedItems);
    }

    public SettlementResultResponseDto confirmReservedFunding(SettlementBatchRequestDto dto) {
        List<Long> confirmedOrderIds = new ArrayList<>();
        List<SettlementResultItem> successItems = new ArrayList<>();
        List<SettlementResultItem> failedItems = new ArrayList<>();

        for (SettlementItem item : dto.items()) {
            try {
                Long walletId = walletService.getWalletId(item.memberId());
                PaymentContext ctx = PaymentContext.forReserved(item, dto.productId(), walletId);

                PaymentResult result;
                try {
                    result = paymentProcessor.process(ctx);
                } catch (BusinessException e) {
                    if (isUserFault(e)) {
                        failureHandler.handleUserFailure(ctx, e);
                        failedItems.add(new SettlementResultItem(item.orderId(), false, e.getMessage()));
                        continue;
                    }
                    throw e;
                }

                // 🆕 Success 또는 AlreadyPaid → 정산 진행
                if (result instanceof PaymentResult.Success || result instanceof PaymentResult.AlreadyPaid) {
                    long amount = settlementProcessor.processSettlementItem(item);
                    confirmedOrderIds.add(item.orderId());
                    if (amount == 0L) {
                        successItems.add(new SettlementResultItem(item.orderId(), true, "ALREADY_CONFIRMED"));
                    } else {
                        successItems.add(new SettlementResultItem(item.orderId(), true, "SUCCESS"));
                    }
                } else {
                    // MaxRetryExceeded 등
                    failedItems.add(new SettlementResultItem(item.orderId(), false, "PAYMENT_FAILED"));
                }
            } catch (Exception e) {
                log.error("Settlement failed. orderId={}", item.orderId(), e);
                failedItems.add(new SettlementResultItem(item.orderId(), false, e.getMessage()));
                eventPublisher.publishOrderCancelled(item.orderId(), CancelReason.PAYMENT_SERVER_ERROR.name());
            }
        }

        try {
            settlementProcessor.creditCreatorForBatch(dto.creatorMemberId(), dto.productId(), "정산 확정 (예약펀딩)");
        } catch (Exception e) {
            log.error("Creator credit failed. batchId={}, creatorId={}, productId={}",
                    dto.batchId(), dto.creatorMemberId(), dto.productId(), e);
            throw e;
        }

        if (!confirmedOrderIds.isEmpty()) {
            eventPublisher.publishOrderProcessConfirmed(confirmedOrderIds);
        }

        return new SettlementResultResponseDto(dto.batchId(), successItems, failedItems);
    }

    public SettlementResultResponseDto refundFailedFunding(SettlementBatchRequestDto dto) {
        List<SettlementResultItem> successItems = new ArrayList<>();
        List<SettlementResultItem> failedItems = new ArrayList<>();
        List<Long> refundedOrderIds = new ArrayList<>();

        for (SettlementItem item : dto.items()) {
            try {
                settlementProcessor.processRefundItem(item.orderId(), item.memberId(), "FUNDING_FAILED");
                successItems.add(new SettlementResultItem(item.orderId(), true, "SUCCESS"));
                refundedOrderIds.add(item.orderId());

            } catch (Exception e) {
                if (e instanceof BusinessException be && be.getErrorCode() == ErrorCode.FUNDING_ALREADY_REFUNDED) {
                    successItems.add(new SettlementResultItem(item.orderId(), true, "ALREADY_REFUNDED"));
                    continue;
                }
                failedItems.add(new SettlementResultItem(item.orderId(), false, e.getMessage()));
            }
        }

        // 환불 완료 이벤트 발행 (성공 건들)
        if (!refundedOrderIds.isEmpty()) {
            eventPublisher.publishOrderProcessRefunded(refundedOrderIds);
        }

        return new SettlementResultResponseDto(dto.batchId(), successItems, failedItems);
    }

    private boolean isUserFault(BusinessException e) {
        return e.getErrorCode() == ErrorCode.WALLET_INSUFFICIENT_BALANCE;
    }
}