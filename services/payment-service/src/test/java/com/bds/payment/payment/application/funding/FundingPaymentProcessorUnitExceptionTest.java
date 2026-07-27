package com.bds.payment.payment.application.funding;

import com.bds.payment.payment.application.wallet.WalletService;
import com.bds.payment.payment.domain.common.PaymentType;
import com.bds.payment.payment.domain.fundingPayment.FundingPayment;
import com.bds.payment.payment.domain.fundingPayment.FundingPaymentRepository;
import com.bds.payment.payment.domain.paymentHistory.PaymentHistoryRepository;
import com.bds.payment.payment.domain.wallet.Wallet;
import com.bds.payment.payment.global.exception.BusinessException;
import com.bds.payment.payment.global.exception.ErrorCode;
import com.bds.payment.payment.presentation.request.FundingPaymentRequestDto;
import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FundingPaymentProcessorUnitExceptionTest {

    @Mock private FundingPaymentRepository fundingPaymentRepository;
    @Mock private PaymentHistoryRepository paymentHistoryRepository;
    @Mock private WalletService walletService;
    @Mock private FundingEventPublisher eventPublisher;

    @InjectMocks
    private FundingPaymentProcessor processor;

    @Nested
    @DisplayName("process() 예외")
    class ProcessExceptionTest {

        @Test
        void 잔액_부족시_예외를_전파하고_저장하지_않는다() {
            // given
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(1L, 1L, 100L, 10000L, PaymentType.INSTANT);
            PaymentContext ctx = PaymentContext.forInstant(dto, 1L);

            given(fundingPaymentRepository.findByOrderId(ctx.orderId())).willReturn(Optional.empty());
            given(walletService.decrease(ctx.memberId(), ctx.amount())).willThrow(new BusinessException(ErrorCode.WALLET_INSUFFICIENT_BALANCE));

            // when & then
            assertThatThrownBy(() -> processor.process(ctx))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_INSUFFICIENT_BALANCE);
                    });

            // 예외 전파되어 저장 로직 실행 안 됨
            verify(fundingPaymentRepository, never()).save(any(FundingPayment.class));
            verify(paymentHistoryRepository, never()).save(any());
            verify(eventPublisher, never()).publishOrderCancelled(any(), anyString());
        }

        @Test
        void 재시도_결제에서_잔액_부족시_예외를_전파한다() {
            // given
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(1L, 1L, 100L, 10000L, PaymentType.INSTANT);
            PaymentContext ctx = PaymentContext.forInstant(dto, 1L);

            FundingPayment existing = FundingPayment.create(dto, 1L, UuidCreator.getTimeOrderedEpoch());
            existing.markFailed();

            given(fundingPaymentRepository.findByOrderId(ctx.orderId())).willReturn(Optional.of(existing));
            given(walletService.decrease(ctx.memberId(), ctx.amount()))
                    .willThrow(new BusinessException(ErrorCode.WALLET_INSUFFICIENT_BALANCE));

            // when & then
            assertThatThrownBy(() -> processor.process(ctx))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_INSUFFICIENT_BALANCE);
                    });

            verify(fundingPaymentRepository, never()).save(any(FundingPayment.class));
            verify(paymentHistoryRepository, never()).save(any());
        }

        @Test
        void 지갑이_존재하지_않으면_예외를_전파한다() {
            // given
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(1L, 1L, 100L, 10000L, PaymentType.INSTANT);
            PaymentContext ctx = PaymentContext.forInstant(dto, 1L);

            given(fundingPaymentRepository.findByOrderId(ctx.orderId())).willReturn(Optional.empty());
            given(walletService.decrease(ctx.memberId(), ctx.amount())).willThrow(new BusinessException(ErrorCode.WALLET_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> processor.process(ctx)).isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_NOT_FOUND);
                    });

            verify(fundingPaymentRepository, never()).save(any(FundingPayment.class));
            verify(paymentHistoryRepository, never()).save(any());
        }

        @Test
        void walletService에서_예상치_못한_예외가_발생하면_그대로_전파한다() {
            // given
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(1L, 1L, 100L, 10000L, PaymentType.INSTANT);
            PaymentContext ctx = PaymentContext.forInstant(dto, 1L);

            given(fundingPaymentRepository.findByOrderId(ctx.orderId())).willReturn(Optional.empty());
            given(walletService.decrease(ctx.memberId(), ctx.amount())).willThrow(new RuntimeException("DB 연결 실패"));

            // when & then
            assertThatThrownBy(() -> processor.process(ctx))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB 연결 실패");

            verify(fundingPaymentRepository, never()).save(any(FundingPayment.class));
        }

        @Test
        void 결제_저장_실패시_예외를_전파한다() {
            // given
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(1L, 1L, 100L, 10000L, PaymentType.INSTANT);
            PaymentContext ctx = PaymentContext.forInstant(dto, 1L);

            Wallet updatedWallet = Wallet.builder().id(1L).memberId(1L).balance(20000L).build();

            given(fundingPaymentRepository.findByOrderId(ctx.orderId())).willReturn(Optional.empty());
            given(walletService.decrease(ctx.memberId(), ctx.amount())).willReturn(updatedWallet);
            given(fundingPaymentRepository.save(any(FundingPayment.class))).willThrow(new RuntimeException("DB 저장 실패"));

            // when & then
            assertThatThrownBy(() -> processor.process(ctx))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB 저장 실패");

            verify(walletService).decrease(ctx.memberId(), ctx.amount());
            verify(paymentHistoryRepository, never()).save(any());
        }
    }
}