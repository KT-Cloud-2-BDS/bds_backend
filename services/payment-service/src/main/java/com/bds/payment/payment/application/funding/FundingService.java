package com.bds.payment.payment.application.funding;

import com.bds.payment.payment.application.payment.PaymentHistoryCommand;
import com.bds.payment.payment.application.wallet.WalletService;
import com.bds.payment.payment.domain.common.PaymentHistoryStatus;
import com.bds.payment.payment.domain.common.PaymentType;
import com.bds.payment.payment.domain.common.TransactionReason;
import com.bds.payment.payment.domain.common.TransactionType;
import com.bds.payment.payment.domain.fundingPayment.FundingPayment;
import com.bds.payment.payment.domain.fundingPayment.FundingPaymentRepository;
import com.bds.payment.payment.domain.paymentHistory.PaymentHistory;
import com.bds.payment.payment.domain.paymentHistory.PaymentHistoryRepository;
import com.bds.payment.payment.domain.wallet.Wallet;
import com.bds.payment.payment.global.exception.BusinessException;
import com.bds.payment.payment.global.exception.ErrorCode;
import com.bds.payment.payment.presentation.request.FundingPaymentRequestDto;
import com.bds.payment.payment.presentation.request.RefundRequestDto;
import com.bds.payment.payment.presentation.request.SettlementBatchRequestDto;
import com.bds.payment.payment.presentation.request.SettlementBatchRequestDto.SettlementItem;
import com.bds.payment.payment.presentation.response.FundingPaymentResponseDto;
import com.bds.payment.payment.presentation.response.SettlementResultResponseDto;
import com.bds.payment.payment.presentation.response.SettlementResultResponseDto.SettlementResultItem;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FundingService {

    private final FundingPaymentRepository fundingPaymentRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final WalletService walletService;
    private final FundingSettlementProcessor processor;
    private final FundingEventPublisher eventPublisher;

    @Transactional
    public FundingPaymentResponseDto funding(FundingPaymentRequestDto dto) {
        validateDuplicatedOrder(dto.orderId());

        UUID tranSeqNo = UuidCreator.getTimeOrderedEpoch();
        Long fromWalletId = walletService.getWalletId(dto.memberId());

        FundingPayment savedFunding = fundingPaymentRepository.save(
                FundingPayment.create(dto, fromWalletId, tranSeqNo)
        );

        if (dto.paymentType() == PaymentType.INSTANT) {
            Wallet updateWallet = walletService.decrease(dto.memberId(), dto.amount());

            PaymentHistoryCommand command = PaymentHistoryCommand.ofFunding(
                    fromWalletId,
                    savedFunding.getId(),
                    tranSeqNo,
                    TransactionType.WITHDRAWAL,
                    TransactionReason.FUNDING_PAYMENT,
                    "즉시펀딩 결제",
                    dto.amount(),
                    updateWallet.getBalance(),
                    PaymentHistoryStatus.SUCCESS
            );
            paymentHistoryRepository.save(PaymentHistory.create(command));

            // INSTANT 결제 성공 이벤트 발행
            eventPublisher.publishOrderPaid(dto.orderId());
        }

        return FundingPaymentResponseDto.from(savedFunding, dto.memberId());
    }

    @Transactional
    public void refund(RefundRequestDto dto) {
        processor.processRefundItem(dto.orderId(), dto.memberId(), dto.cancelReason());

        // 환불 완료 이벤트 발행
        eventPublisher.publishOrderProcessRefunded(List.of(dto.orderId()));
    }

    @Transactional
    public SettlementResultResponseDto confirmSettlement(SettlementBatchRequestDto dto) {
        List<SettlementResultItem> successItems = new ArrayList<>();
        List<SettlementResultItem> failedItems = new ArrayList<>();
        List<Long> confirmedOrderIds = new ArrayList<>();

        for (SettlementItem item : dto.items()) {
            try {
                long amount = processor.processSettlementItem(item);

                if (amount == 0L) {
                    successItems.add(new SettlementResultItem(item.orderId(), true, "ALREADY_CONFIRMED"));
                } else {
                    successItems.add(new SettlementResultItem(item.orderId(), true, null));
                }
                confirmedOrderIds.add(item.orderId());
            } catch (Exception e) {
                log.error("Settlement failed. orderId={}", item.orderId(), e);
                failedItems.add(new SettlementResultItem(item.orderId(), false, e.getMessage()));

                // 실패 이벤트 발행
                eventPublisher.publishOrderCancelled(item.orderId(), e.getMessage());
            }
        }

        try {
            processor.creditCreatorForBatch(dto.creatorMemberId(), dto.productId(), "정산 확정 (즉시펀딩)");
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

    @Transactional
    public SettlementResultResponseDto confirmReservedFunding(SettlementBatchRequestDto dto) {
        List<SettlementResultItem> successItems = new ArrayList<>();
        List<SettlementResultItem> failedItems = new ArrayList<>();
        List<Long> confirmedOrderIds = new ArrayList<>();

        for (SettlementItem item : dto.items()) {
            try {
                long amount = processor.processReservedFundingItem(item, dto.productId());

                if (amount == 0L) {
                    successItems.add(new SettlementResultItem(item.orderId(), true, "ALREADY_CONFIRMED"));
                } else {
                    successItems.add(new SettlementResultItem(item.orderId(), true, null));
                }
                confirmedOrderIds.add(item.orderId());
            } catch (Exception e) {
                log.error("Settlement failed. orderId={}", item.orderId(), e);
                failedItems.add(new SettlementResultItem(item.orderId(), false, e.getMessage()));

                // 실패 이벤트 발행
                eventPublisher.publishOrderCancelled(item.orderId(), e.getMessage());
            }
        }

        try {
            processor.creditCreatorForBatch(dto.creatorMemberId(), dto.productId(), "정산 확정 (예약펀딩)");
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

    @Transactional
    public SettlementResultResponseDto refundFailedFunding(SettlementBatchRequestDto dto) {
        List<SettlementResultItem> successItems = new ArrayList<>();
        List<SettlementResultItem> failedItems = new ArrayList<>();
        List<Long> refundedOrderIds = new ArrayList<>();

        for (SettlementItem item : dto.items()) {
            try {
                processor.processRefundItem(item.orderId(), item.memberId(), "FUNDING_FAILED");
                successItems.add(new SettlementResultItem(item.orderId(), true, null));
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

    private void validateDuplicatedOrder(Long orderId) {
        if (fundingPaymentRepository.existsByOrderId(orderId)) {
            throw new BusinessException(ErrorCode.FUNDING_DUPLICATED);
        }
    }
}