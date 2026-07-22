package com.bds.payment.payment.application.funding;

import com.bds.payment.payment.application.payment.PaymentHistoryCommand;
import com.bds.payment.payment.application.wallet.WalletService;
import com.bds.payment.payment.domain.common.*;
import com.bds.payment.payment.domain.fundingPayment.FundingPayment;
import com.bds.payment.payment.domain.fundingPayment.FundingPaymentRepository;
import com.bds.payment.payment.domain.paymentHistory.PaymentHistory;
import com.bds.payment.payment.domain.paymentHistory.PaymentHistoryRepository;
import com.bds.payment.payment.domain.wallet.Wallet;
import com.bds.payment.payment.global.exception.BusinessException;
import com.bds.payment.payment.global.exception.ErrorCode;
import com.bds.payment.payment.presentation.request.SettlementBatchRequestDto.SettlementItem;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FundingSettlementProcessor {

    private final FundingPaymentRepository fundingPaymentRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final WalletService walletService;

    /**
     * 즉시펀딩 단건 확정 (SUCCESS → CONFIRMED)
     * @return 확정된 금액
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long processSettlementItem(SettlementItem item) {
        FundingPayment fp = fundingPaymentRepository.findByOrderId(item.orderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FUNDING_NOT_FOUND));

        if (fp.getStatus() == FundingPaymentStatus.CONFIRMED) {
            return 0L;
        }

        if (!fp.getAmount().equals(item.amount())) {
            throw new BusinessException(ErrorCode.SETTLEMENT_AMOUNT_MISMATCH);
        }

        fp.confirm();
        fundingPaymentRepository.save(fp);
        return fp.getAmount();
    }

    /**
     * 예약펀딩 단건 확정 (신규 생성 + 청구)
     * @return 청구된 금액
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long processReservedFundingItem(SettlementItem item, Long productId) {
        if (fundingPaymentRepository.existsByOrderId(item.orderId())) {
            return 0L;
        }

        Wallet buyerWallet = walletService.decrease(item.memberId(), item.amount());
        Long buyerWalletId = buyerWallet.getId();
        UUID tranSeqNo = UuidCreator.getTimeOrderedEpoch();

        FundingPayment fp = FundingPayment.builder()
                .orderId(item.orderId())
                .walletId(buyerWalletId)
                .productId(productId)
                .tranSeqNo(tranSeqNo)
                .amount(item.amount())
                .paymentType(PaymentType.RESERVED)
                .status(FundingPaymentStatus.CONFIRMED)
                .build();
        FundingPayment savedFp = fundingPaymentRepository.save(fp);

        PaymentHistoryCommand buyerCommand = PaymentHistoryCommand.ofFunding(
                buyerWalletId,
                savedFp.getId(),
                tranSeqNo,
                TransactionType.WITHDRAWAL,
                TransactionReason.FUNDING_PAYMENT,
                "예약펀딩 청구",
                item.amount(),
                buyerWallet.getBalance(),
                PaymentHistoryStatus.SUCCESS
        );
        paymentHistoryRepository.save(PaymentHistory.create(buyerCommand));

        return item.amount();
    }

    /**
     * 환불 처리 (단건/배치 공통)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processRefundItem(Long orderId, Long memberId, String cancelReason) {
        FundingPayment fp = fundingPaymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FUNDING_NOT_FOUND));

        Long walletId = walletService.getWalletId(memberId);

        if (!fp.getWalletId().equals(walletId)) {
            throw new BusinessException(ErrorCode.FUNDING_ACCESS_DENIED);
        }
        if (fp.getStatus() == FundingPaymentStatus.REFUNDED) {
            throw new BusinessException(ErrorCode.FUNDING_ALREADY_REFUNDED);
        }
        if (fp.getStatus() != FundingPaymentStatus.SUCCESS
                && fp.getStatus() != FundingPaymentStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.FUNDING_INVALID_STATUS);
        }

        if (fp.getPaymentType() == PaymentType.INSTANT) {
            Wallet updateWallet = walletService.charge(memberId, fp.getAmount());

            PaymentHistoryCommand command = PaymentHistoryCommand.ofFunding(
                    walletId,
                    fp.getId(),
                    UuidCreator.getTimeOrderedEpoch(),
                    TransactionType.DEPOSIT,
                    TransactionReason.FUNDING_REFUND,
                    cancelReason,
                    fp.getAmount(),
                    updateWallet.getBalance(),
                    PaymentHistoryStatus.SUCCESS
            );
            paymentHistoryRepository.save(PaymentHistory.create(command));
        }

        fp.refund();
        fundingPaymentRepository.save(fp);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void creditCreatorForBatch(Long creatorMemberId, Long productId, String message) {
        List<FundingPayment> uncredited = fundingPaymentRepository
                .findUncreditedForUpdate(productId, FundingPaymentStatus.CONFIRMED);

        if (uncredited.isEmpty()) {
            return;  // 이미 다 크레딧됨 (멱등)
        }

        long total = uncredited.stream().mapToLong(FundingPayment::getAmount).sum();
        Wallet creatorWallet = walletService.charge(creatorMemberId, total);

        LocalDateTime now = LocalDateTime.now();
        uncredited.forEach(fp -> fp.markCredited(now));
        fundingPaymentRepository.saveAll(uncredited);

        PaymentHistoryCommand command = PaymentHistoryCommand.ofSettlement(
                creatorWallet,
                UuidCreator.getTimeOrderedEpoch(),
                message,
                total,
                PaymentHistoryStatus.SUCCESS
        );
        paymentHistoryRepository.save(PaymentHistory.create(command));
    }
}