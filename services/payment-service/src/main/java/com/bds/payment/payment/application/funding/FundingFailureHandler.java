package com.bds.payment.payment.application.funding;

import com.bds.payment.payment.application.payment.PaymentHistoryCommand;
import com.bds.payment.payment.domain.common.CancelReason;
import com.bds.payment.payment.domain.common.PaymentHistoryStatus;
import com.bds.payment.payment.domain.common.TransactionReason;
import com.bds.payment.payment.domain.common.TransactionType;
import com.bds.payment.payment.domain.fundingPayment.FundingPayment;
import com.bds.payment.payment.domain.fundingPayment.FundingPaymentRepository;
import com.bds.payment.payment.domain.paymentHistory.PaymentHistory;
import com.bds.payment.payment.domain.paymentHistory.PaymentHistoryRepository;
import com.bds.payment.payment.global.exception.BusinessException;
import com.bds.payment.payment.presentation.request.FundingPaymentRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class FundingFailureHandler {

    private final FundingPaymentRepository fundingPaymentRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final FundingEventPublisher eventPublisher;

    /**
     * 유저 귀책 실패 처리 (잔액 부족 등)
     * - retryCnt 증가 및 FAILED 상태 저장
     * - 실패 이력 저장
     * - 재시도 가능 여부에 따라 이벤트 분기 발행
     * 별도 트랜잭션(REQUIRES_NEW)으로 실행하여
     * 상위 트랜잭션의 rollback-only 마크와 분리
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleUserFailure(
            FundingPayment funding,
            FundingPaymentRequestDto dto,
            BusinessException e
    ) {
        // 1. 실패 상태 및 retryCnt 증가
        funding.markFailed();
        FundingPayment saved = fundingPaymentRepository.save(funding);

        // 2. 실패 이력 저장
        PaymentHistoryCommand command = PaymentHistoryCommand.ofFunding(
                saved.getWalletId(),
                saved.getId(),
                saved.getTranSeqNo(),
                TransactionType.WITHDRAWAL,
                TransactionReason.FUNDING_PAYMENT,
                "결제 실패: " + e.getErrorCode().name(),
                dto.amount(),
                0L,  // 잔액 변동 없음
                PaymentHistoryStatus.FAILED
        );
        paymentHistoryRepository.save(PaymentHistory.create(command));

        // 3. 이벤트 발행 (재시도 초과 vs 재시도 가능)
        String reason = saved.isMaxRetryExceeded()
                ? CancelReason.MAX_RETRY_EXCEEDED.name()
                : CancelReason.INSUFFICIENT_BALANCE.name();

        eventPublisher.publishOrderCancelled(dto.orderId(), reason);

        log.info("User failure handled. orderId={}, retryCnt={}, reason={}",
                dto.orderId(), saved.getRetryCnt(), reason);
    }

    /**
     * 시스템 오류 처리
     * - funding_payment는 저장하지 않음 (상위 트랜잭션 롤백에 맡김)
     * - 주문 서비스에 즉시 통지
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleSystemError(Long orderId) {
        eventPublisher.publishOrderCancelled(
                orderId,
                CancelReason.PAYMENT_SERVER_ERROR.name()
        );

        log.error("System error notified. orderId={}", orderId);
    }
}