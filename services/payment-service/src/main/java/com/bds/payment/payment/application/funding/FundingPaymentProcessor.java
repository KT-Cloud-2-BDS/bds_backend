package com.bds.payment.payment.application.funding;

import com.bds.payment.payment.application.payment.PaymentHistoryCommand;
import com.bds.payment.payment.application.wallet.WalletService;
import com.bds.payment.payment.domain.common.*;
import com.bds.payment.payment.domain.fundingPayment.FundingPayment;
import com.bds.payment.payment.domain.fundingPayment.FundingPaymentRepository;
import com.bds.payment.payment.domain.paymentHistory.PaymentHistory;
import com.bds.payment.payment.domain.paymentHistory.PaymentHistoryRepository;
import com.bds.payment.payment.domain.wallet.Wallet;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class FundingPaymentProcessor {

    private final FundingPaymentRepository fundingPaymentRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final WalletService walletService;
    private final FundingEventPublisher eventPublisher;
    private final FundingFailureHandler failureHandler;

    /**
     * 결제 처리 공통 로직 (INSTANT/RESERVED 공통)
     * - 신규 결제 or 재시도 결제 모두 처리
     * - 성공 시 SUCCESS 상태로 저장 (CONFIRMED 전이는 호출자 책임)
     */
    @Transactional
    public PaymentResult process(PaymentContext ctx) {
        FundingPayment funding = fundingPaymentRepository.findByOrderId(ctx.orderId())
                .orElseGet(() -> FundingPayment.create(ctx.toDto(), ctx.walletId(), UuidCreator.getTimeOrderedEpoch()));

        if (funding.getStatus() == FundingPaymentStatus.SUCCESS || funding.getStatus() == FundingPaymentStatus.CONFIRMED) {
            return new PaymentResult.AlreadyPaid(funding);
        }

        if (funding.isMaxRetryExceeded()) {
            eventPublisher.publishOrderCancelled(ctx.orderId(), CancelReason.MAX_RETRY_EXCEEDED.name());
            return new PaymentResult.MaxRetryExceeded(funding);
        }

        // 결제 시도 (실패 시 트랜잭션 롤백 후 예외 전파)
        Wallet updateWallet = walletService.decrease(ctx.memberId(), ctx.amount());

        funding.markSuccess();
        FundingPayment saved = fundingPaymentRepository.save(funding);
        savePaymentHistory(saved, updateWallet, ctx);

        return new PaymentResult.Success(saved);
    }

    private void savePaymentHistory(FundingPayment funding, Wallet wallet, PaymentContext ctx) {
        PaymentHistoryCommand command = PaymentHistoryCommand.ofFunding(
                ctx.walletId(),
                funding.getId(),
                UuidCreator.getTimeOrderedEpoch(),
                TransactionType.WITHDRAWAL,
                TransactionReason.FUNDING_PAYMENT,
                ctx.historyMessage(),
                ctx.amount(),
                wallet.getBalance(),
                PaymentHistoryStatus.SUCCESS
        );
        paymentHistoryRepository.save(PaymentHistory.create(command));
    }
}