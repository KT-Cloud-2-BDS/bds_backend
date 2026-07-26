package com.bds.payment.payment.application.funding;

import com.bds.payment.payment.application.payment.PaymentHistoryCommand;
import com.bds.payment.payment.application.wallet.WalletService;
import com.bds.payment.payment.domain.common.CancelReason;
import com.bds.payment.payment.domain.common.PaymentHistoryStatus;
import com.bds.payment.payment.domain.common.TransactionReason;
import com.bds.payment.payment.domain.common.TransactionType;
import com.bds.payment.payment.domain.fundingPayment.FundingPayment;
import com.bds.payment.payment.domain.fundingPayment.FundingPaymentRepository;
import com.bds.payment.payment.domain.paymentHistory.PaymentHistory;
import com.bds.payment.payment.domain.paymentHistory.PaymentHistoryRepository;
import com.bds.payment.payment.global.exception.BusinessException;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class FundingFailureHandler {

    private final FundingPaymentRepository fundingPaymentRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final WalletService walletService;
    private final FundingEventPublisher eventPublisher;

    /**
     * 유저 귀책 실패 처리 (잔액 부족 등)
     * - retryCnt 증가 및 FAILED 상태 저장
     * - 실패 이력 저장
     * - 재시도 가능 여부에 따라 이벤트 분기 발행
     */
    @Transactional
    public void handleUserFailure(PaymentContext ctx, BusinessException e) {
        // 재조회 or 신규 생성
        FundingPayment funding = fundingPaymentRepository.findByOrderId(ctx.orderId()).orElseGet(() ->
                FundingPayment.create(ctx.toDto(), ctx.walletId(), UuidCreator.getTimeOrderedEpoch()));

        funding.markFailed();
        FundingPayment saved = fundingPaymentRepository.save(funding);

        Long currentBalance = walletService.getBalance(ctx.memberId());

        // 실패 이력 저장
        PaymentHistoryCommand command = PaymentHistoryCommand.ofFunding(
                saved.getWalletId(),
                saved.getId(),
                UuidCreator.getTimeOrderedEpoch(),
                TransactionType.WITHDRAWAL,
                TransactionReason.FUNDING_PAYMENT,
                "결제 실패: " + e.getErrorCode().name(),
                ctx.amount(),
                currentBalance,
                PaymentHistoryStatus.FAILED
        );
        paymentHistoryRepository.save(PaymentHistory.create(command));

        // 이벤트 발행
        String reason = saved.isMaxRetryExceeded()
                ? CancelReason.MAX_RETRY_EXCEEDED.name()
                : CancelReason.INSUFFICIENT_BALANCE.name();

        eventPublisher.publishOrderCancelled(ctx.orderId(), reason);

        log.info("User failure handled. orderId={}, retryCnt={}, reason={}", ctx.orderId(), saved.getRetryCnt(), reason);
    }

    /**
     * 시스템 오류 처리
     * - 주문 서비스에 즉시 통지
     */
    @Transactional
    public void handleSystemError(Long orderId) {
        eventPublisher.publishOrderCancelled(
                orderId,
                CancelReason.PAYMENT_SERVER_ERROR.name()
        );

        log.error("System error notified. orderId={}", orderId);
    }
}