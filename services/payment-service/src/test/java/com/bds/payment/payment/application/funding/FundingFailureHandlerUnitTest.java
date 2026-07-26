package com.bds.payment.payment.application.funding;

import com.bds.payment.payment.domain.common.CancelReason;
import com.bds.payment.payment.domain.common.FundingPaymentStatus;
import com.bds.payment.payment.domain.common.PaymentType;
import com.bds.payment.payment.domain.fundingPayment.FundingPayment;
import com.bds.payment.payment.domain.fundingPayment.FundingPaymentRepository;
import com.bds.payment.payment.domain.paymentHistory.PaymentHistory;
import com.bds.payment.payment.domain.paymentHistory.PaymentHistoryRepository;
import com.bds.payment.payment.global.exception.BusinessException;
import com.bds.payment.payment.global.exception.ErrorCode;
import com.bds.payment.payment.presentation.request.FundingPaymentRequestDto;
import com.bds.payment.payment.presentation.request.SettlementBatchRequestDto.SettlementItem;
import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class FundingFailureHandlerUnitTest {

    @Mock private FundingPaymentRepository fundingPaymentRepository;
    @Mock private PaymentHistoryRepository paymentHistoryRepository;
    @Mock private FundingEventPublisher eventPublisher;

    @InjectMocks
    private FundingFailureHandler failureHandler;

    @Nested
    @DisplayName("handleUserFailure()")
    class HandleUserFailureTest {

        @Test
        void 신규_결제_실패시_FAILED_상태로_저장하고_INSUFFICIENT_BALANCE_이벤트를_발행한다() {
            // given
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(
                    1L, 1L, 100L, 10000L, PaymentType.INSTANT
            );
            PaymentContext ctx = PaymentContext.forInstant(dto, 1L);
            BusinessException e = new BusinessException(ErrorCode.WALLET_INSUFFICIENT_BALANCE);

            given(fundingPaymentRepository.findByOrderId(ctx.orderId())).willReturn(Optional.empty());
            given(fundingPaymentRepository.save(any(FundingPayment.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            failureHandler.handleUserFailure(ctx, e);

            // then
            ArgumentCaptor<FundingPayment> fundingCaptor = ArgumentCaptor.forClass(FundingPayment.class);
            verify(fundingPaymentRepository).save(fundingCaptor.capture());
            FundingPayment saved = fundingCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(FundingPaymentStatus.FAILED);
            assertThat(saved.getRetryCnt()).isEqualTo(1);

            verify(paymentHistoryRepository).save(any(PaymentHistory.class));
            verify(eventPublisher).publishOrderCancelled(
                    eq(ctx.orderId()),
                    eq(CancelReason.INSUFFICIENT_BALANCE.name())
            );
        }

        @Test
        void 재시도_실패시_기존_record의_retryCnt가_증가한다() {
            // given: 기존 FAILED record 존재 (retryCnt=1)
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(
                    1L, 1L, 100L, 10000L, PaymentType.INSTANT
            );
            PaymentContext ctx = PaymentContext.forInstant(dto, 1L);
            BusinessException e = new BusinessException(ErrorCode.WALLET_INSUFFICIENT_BALANCE);

            FundingPayment existing = FundingPayment.create(dto, 1L, UuidCreator.getTimeOrderedEpoch());
            existing.markFailed();  // retryCnt=1

            given(fundingPaymentRepository.findByOrderId(ctx.orderId())).willReturn(Optional.of(existing));
            given(fundingPaymentRepository.save(any(FundingPayment.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            failureHandler.handleUserFailure(ctx, e);

            // then
            assertThat(existing.getRetryCnt()).isEqualTo(2);  // 1 → 2
            assertThat(existing.getStatus()).isEqualTo(FundingPaymentStatus.FAILED);

            verify(fundingPaymentRepository).save(existing);
            verify(eventPublisher).publishOrderCancelled(
                    eq(ctx.orderId()),
                    eq(CancelReason.INSUFFICIENT_BALANCE.name())
            );
        }

        @Test
        void MAX_RETRY_초과시_MAX_RETRY_EXCEEDED_이벤트를_발행한다() {
            // given: retryCnt=2인 record → markFailed 후 3이 되어 초과
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(1L, 1L, 100L, 10000L, PaymentType.INSTANT);
            PaymentContext ctx = PaymentContext.forInstant(dto, 1L);
            BusinessException e = new BusinessException(ErrorCode.WALLET_INSUFFICIENT_BALANCE);

            FundingPayment existing = FundingPayment.create(dto, 1L, UuidCreator.getTimeOrderedEpoch());
            existing.markFailed();  // retryCnt=1
            existing.markFailed();  // retryCnt=2

            given(fundingPaymentRepository.findByOrderId(ctx.orderId())).willReturn(Optional.of(existing));
            given(fundingPaymentRepository.save(any(FundingPayment.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            failureHandler.handleUserFailure(ctx, e);

            // then
            assertThat(existing.getRetryCnt()).isEqualTo(3);  // 2 → 3 (MAX)

            verify(eventPublisher).publishOrderCancelled(
                    eq(ctx.orderId()),
                    eq(CancelReason.MAX_RETRY_EXCEEDED.name())  // ← MAX_RETRY_EXCEEDED
            );
        }

        @Test
        void 실패_이력이_FAILED_상태로_저장된다() {
            // given
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(
                    1L, 1L, 100L, 10000L, PaymentType.INSTANT
            );
            PaymentContext ctx = PaymentContext.forInstant(dto, 1L);
            BusinessException e = new BusinessException(ErrorCode.WALLET_INSUFFICIENT_BALANCE);

            given(fundingPaymentRepository.findByOrderId(ctx.orderId())).willReturn(Optional.empty());
            given(fundingPaymentRepository.save(any(FundingPayment.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            // when
            failureHandler.handleUserFailure(ctx, e);

            // then
            verify(paymentHistoryRepository).save(any(PaymentHistory.class));
            // PaymentHistory 세부 검증은 통합 테스트에서 확인
        }

        @Test
        void RESERVED_타입_결제_실패도_정상_처리된다() {
            // given
            SettlementItem item = new SettlementItem(1L, 1L, 10000L);
            PaymentContext ctx = PaymentContext.forReserved(item, 100L, 1L);
            BusinessException e = new BusinessException(ErrorCode.WALLET_INSUFFICIENT_BALANCE);

            given(fundingPaymentRepository.findByOrderId(ctx.orderId())).willReturn(Optional.empty());
            given(fundingPaymentRepository.save(any(FundingPayment.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            failureHandler.handleUserFailure(ctx, e);

            // then
            ArgumentCaptor<FundingPayment> captor = ArgumentCaptor.forClass(FundingPayment.class);
            verify(fundingPaymentRepository).save(captor.capture());
            assertThat(captor.getValue().getPaymentType()).isEqualTo(PaymentType.RESERVED);
            assertThat(captor.getValue().getStatus()).isEqualTo(FundingPaymentStatus.FAILED);

            verify(eventPublisher).publishOrderCancelled(any(), anyString());
        }
    }

    @Nested
    @DisplayName("handleSystemError()")
    class HandleSystemErrorTest {

        @Test
        void 시스템_오류시_PAYMENT_SERVER_ERROR_이벤트를_발행한다() {
            // given
            Long orderId = 1L;

            // when
            failureHandler.handleSystemError(orderId);

            // then
            verify(eventPublisher).publishOrderCancelled(eq(orderId), eq(CancelReason.PAYMENT_SERVER_ERROR.name()));
        }

        @Test
        void 시스템_오류시_funding_payment는_저장하지_않는다() {
            // given
            Long orderId = 1L;

            // when
            failureHandler.handleSystemError(orderId);

            // then
            verify(fundingPaymentRepository, never()).save(any(FundingPayment.class));
            verify(paymentHistoryRepository, never()).save(any());
        }
    }
}