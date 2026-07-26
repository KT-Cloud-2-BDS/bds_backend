package com.bds.payment.payment.application.funding;

import com.bds.payment.payment.application.wallet.WalletService;
import com.bds.payment.payment.domain.common.PaymentType;
import com.bds.payment.payment.domain.fundingPayment.FundingPayment;
import com.bds.payment.payment.domain.fundingPayment.FundingPaymentRepository;
import com.bds.payment.payment.domain.paymentHistory.PaymentHistory;
import com.bds.payment.payment.domain.paymentHistory.PaymentHistoryRepository;
import com.bds.payment.payment.global.exception.BusinessException;
import com.bds.payment.payment.global.exception.ErrorCode;
import com.bds.payment.payment.presentation.request.FundingPaymentRequestDto;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FundingFailureHandlerUnitExceptionTest {

    @Mock private FundingPaymentRepository fundingPaymentRepository;
    @Mock private PaymentHistoryRepository paymentHistoryRepository;
    @Mock private FundingEventPublisher eventPublisher;
    @Mock private WalletService walletService;

    @InjectMocks
    private FundingFailureHandler failureHandler;

    @Nested
    @DisplayName("handleUserFailure() 예외")
    class HandleUserFailureExceptionTest {

        @Test
        void 잔액_조회_실패시_예외를_전파한다() {
            // given
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(
                    1L, 1L, 100L, 10000L, PaymentType.INSTANT
            );
            PaymentContext ctx = PaymentContext.forInstant(dto, 1L);
            BusinessException e = new BusinessException(ErrorCode.WALLET_INSUFFICIENT_BALANCE);

            given(fundingPaymentRepository.findByOrderId(ctx.orderId())).willReturn(Optional.empty());
            given(fundingPaymentRepository.save(any(FundingPayment.class))).willAnswer(inv -> inv.getArgument(0));
            given(walletService.getBalance(ctx.memberId())).willThrow(new BusinessException(ErrorCode.WALLET_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> failureHandler.handleUserFailure(ctx, e))
                    .isInstanceOfSatisfying(BusinessException.class, ex ->
                            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_NOT_FOUND));

            // funding은 저장됐지만 history 저장, 이벤트 발행은 실행 안 됨
            verify(fundingPaymentRepository).save(any(FundingPayment.class));
            verify(paymentHistoryRepository, never()).save(any());
            verify(eventPublisher, never()).publishOrderCancelled(any(), anyString());
        }

        @Test
        void funding_저장_실패시_예외를_전파한다() {
            // given
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(
                    1L, 1L, 100L, 10000L, PaymentType.INSTANT
            );
            PaymentContext ctx = PaymentContext.forInstant(dto, 1L);
            BusinessException e = new BusinessException(ErrorCode.WALLET_INSUFFICIENT_BALANCE);

            given(fundingPaymentRepository.findByOrderId(ctx.orderId())).willReturn(Optional.empty());
            given(fundingPaymentRepository.save(any(FundingPayment.class)))
                    .willThrow(new RuntimeException("DB 저장 실패"));

            // when & then
            assertThatThrownBy(() -> failureHandler.handleUserFailure(ctx, e))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB 저장 실패");

            verify(paymentHistoryRepository, never()).save(any());
            verify(eventPublisher, never()).publishOrderCancelled(any(), anyString());
        }

        @Test
        void payment_history_저장_실패시_예외를_전파한다() {
            // given
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(
                    1L, 1L, 100L, 10000L, PaymentType.INSTANT
            );
            PaymentContext ctx = PaymentContext.forInstant(dto, 1L);
            BusinessException e = new BusinessException(ErrorCode.WALLET_INSUFFICIENT_BALANCE);

            given(walletService.getBalance(ctx.memberId())).willReturn(5000L);
            given(fundingPaymentRepository.findByOrderId(ctx.orderId())).willReturn(Optional.empty());
            given(fundingPaymentRepository.save(any(FundingPayment.class)))
                    .willAnswer(inv -> inv.getArgument(0));
            given(paymentHistoryRepository.save(any(PaymentHistory.class)))
                    .willThrow(new RuntimeException("이력 저장 실패"));

            // when & then
            assertThatThrownBy(() -> failureHandler.handleUserFailure(ctx, e))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("이력 저장 실패");

            verify(fundingPaymentRepository).save(any(FundingPayment.class));
            verify(eventPublisher, never()).publishOrderCancelled(any(), anyString());
        }

        @Test
        void 이벤트_발행_실패시_예외를_전파한다() {
            // given
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(
                    1L, 1L, 100L, 10000L, PaymentType.INSTANT
            );
            PaymentContext ctx = PaymentContext.forInstant(dto, 1L);
            BusinessException e = new BusinessException(ErrorCode.WALLET_INSUFFICIENT_BALANCE);

            given(walletService.getBalance(ctx.memberId())).willReturn(5000L);
            given(fundingPaymentRepository.findByOrderId(ctx.orderId())).willReturn(Optional.empty());
            given(fundingPaymentRepository.save(any(FundingPayment.class)))
                    .willAnswer(inv -> inv.getArgument(0));
            doThrow(new RuntimeException("이벤트 발행 실패"))
                    .when(eventPublisher).publishOrderCancelled(any(), anyString());

            // when & then
            assertThatThrownBy(() -> failureHandler.handleUserFailure(ctx, e))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("이벤트 발행 실패");

            verify(fundingPaymentRepository).save(any(FundingPayment.class));
            verify(paymentHistoryRepository).save(any(PaymentHistory.class));
        }
    }

    @Nested
    @DisplayName("handleSystemError() 예외")
    class HandleSystemErrorExceptionTest {

        @Test
        void 이벤트_발행_실패시_예외를_전파한다() {
            // given
            Long orderId = 1L;
            doThrow(new RuntimeException("이벤트 발행 실패"))
                    .when(eventPublisher).publishOrderCancelled(any(), anyString());

            // when & then
            assertThatThrownBy(() -> failureHandler.handleSystemError(orderId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("이벤트 발행 실패");
        }
    }
}