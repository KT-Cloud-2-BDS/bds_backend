package com.bds.payment.payment.application.funding;

import com.bds.payment.payment.application.wallet.WalletService;
import com.bds.payment.payment.domain.common.CancelReason;
import com.bds.payment.payment.domain.common.FundingPaymentStatus;
import com.bds.payment.payment.domain.common.PaymentType;
import com.bds.payment.payment.domain.fundingPayment.FundingPayment;
import com.bds.payment.payment.domain.fundingPayment.FundingPaymentRepository;
import com.bds.payment.payment.domain.paymentHistory.PaymentHistory;
import com.bds.payment.payment.domain.paymentHistory.PaymentHistoryRepository;
import com.bds.payment.payment.domain.wallet.Wallet;
import com.bds.payment.payment.presentation.request.FundingPaymentRequestDto;
import com.bds.payment.payment.presentation.request.SettlementBatchRequestDto.SettlementItem;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FundingPaymentProcessorUnitTest {

    @Mock private FundingPaymentRepository fundingPaymentRepository;
    @Mock private PaymentHistoryRepository paymentHistoryRepository;
    @Mock private WalletService walletService;
    @Mock private FundingEventPublisher eventPublisher;

    @InjectMocks
    private FundingPaymentProcessor processor;

    @Nested
    @DisplayName("process()")
    class ProcessTest {

        @Test
        void 신규_결제_성공시_SUCCESS_상태로_저장하고_Success를_리턴한다() {
            // given
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(
                    1L, 1L, 100L, 10000L, PaymentType.INSTANT
            );
            PaymentContext ctx = PaymentContext.forInstant(dto, 1L);

            Wallet updatedWallet = Wallet.builder()
                    .id(1L)
                    .memberId(1L)
                    .balance(20000L)
                    .build();

            given(fundingPaymentRepository.findByOrderId(ctx.orderId())).willReturn(Optional.empty());
            given(walletService.decrease(ctx.memberId(), ctx.amount())).willReturn(updatedWallet);
            given(fundingPaymentRepository.save(any(FundingPayment.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            PaymentResult result = processor.process(ctx);

            // then
            assertThat(result).isInstanceOf(PaymentResult.Success.class);
            assertThat(result.funding().getStatus()).isEqualTo(FundingPaymentStatus.SUCCESS);

            verify(walletService).decrease(ctx.memberId(), ctx.amount());
            verify(fundingPaymentRepository).save(any(FundingPayment.class));
            verify(paymentHistoryRepository).save(any(PaymentHistory.class));
            verify(eventPublisher, never()).publishOrderCancelled(any(), anyString());
        }

        @Test
        void 재시도_결제_성공시_기존_record가_SUCCESS로_전이된다() {
            // given: FAILED 상태의 기존 record 존재
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(1L, 1L, 100L, 10000L, PaymentType.INSTANT);
            PaymentContext ctx = PaymentContext.forInstant(dto, 1L);

            FundingPayment existing = FundingPayment.create(dto, 1L, UuidCreator.getTimeOrderedEpoch());
            existing.markFailed();  // retryCnt=1

            Wallet updatedWallet = Wallet.builder().id(1L).memberId(1L).balance(20000L).build();

            given(fundingPaymentRepository.findByOrderId(ctx.orderId())).willReturn(Optional.of(existing));
            given(walletService.decrease(ctx.memberId(), ctx.amount())).willReturn(updatedWallet);
            given(fundingPaymentRepository.save(any(FundingPayment.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            PaymentResult result = processor.process(ctx);

            // then
            assertThat(result).isInstanceOf(PaymentResult.Success.class);
            assertThat(result.funding().getStatus()).isEqualTo(FundingPaymentStatus.SUCCESS);
            assertThat(result.funding().getRetryCnt()).isEqualTo(1);

            verify(fundingPaymentRepository).save(existing);
            verify(paymentHistoryRepository).save(any(PaymentHistory.class));
            verify(eventPublisher, never()).publishOrderCancelled(any(), anyString());
        }

        @Test
        void 이미_SUCCESS_상태면_AlreadyPaid를_리턴한다() {
            // given
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(
                    1L, 1L, 100L, 10000L, PaymentType.INSTANT
            );
            PaymentContext ctx = PaymentContext.forInstant(dto, 1L);

            FundingPayment existing = FundingPayment.create(dto, 1L, UuidCreator.getTimeOrderedEpoch());
            existing.markSuccess();

            given(fundingPaymentRepository.findByOrderId(ctx.orderId())).willReturn(Optional.of(existing));

            // when
            PaymentResult result = processor.process(ctx);

            // then
            assertThat(result).isInstanceOf(PaymentResult.AlreadyPaid.class);

            verify(walletService, never()).decrease(any(), any());
            verify(fundingPaymentRepository, never()).save(any(FundingPayment.class));
            verify(paymentHistoryRepository, never()).save(any());
        }

        @Test
        void 이미_CONFIRMED_상태면_AlreadyPaid를_리턴한다() {
            // given
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(1L, 1L, 100L, 10000L, PaymentType.INSTANT);
            PaymentContext ctx = PaymentContext.forInstant(dto, 1L);

            FundingPayment existing = FundingPayment.builder()
                    .orderId(1L)
                    .walletId(1L)
                    .productId(100L)
                    .amount(10000L)
                    .paymentType(PaymentType.INSTANT)
                    .status(FundingPaymentStatus.CONFIRMED)
                    .build();

            given(fundingPaymentRepository.findByOrderId(ctx.orderId())).willReturn(Optional.of(existing));

            // when
            PaymentResult result = processor.process(ctx);

            // then
            assertThat(result).isInstanceOf(PaymentResult.AlreadyPaid.class);

            verify(walletService, never()).decrease(any(), any());
            verify(fundingPaymentRepository, never()).save(any(FundingPayment.class));
        }

        @Test
        void 재시도_횟수_초과시_MaxRetryExceeded를_리턴하고_이벤트를_발행한다() {
            // given: retryCnt=3인 record
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(1L, 1L, 100L, 10000L, PaymentType.INSTANT);
            PaymentContext ctx = PaymentContext.forInstant(dto, 1L);

            FundingPayment existing = FundingPayment.create(dto, 1L, UuidCreator.getTimeOrderedEpoch());
            for (int i = 0; i < FundingPayment.MAX_RETRY; i++) {
                existing.markFailed();
            }

            given(fundingPaymentRepository.findByOrderId(ctx.orderId())).willReturn(Optional.of(existing));

            // when
            PaymentResult result = processor.process(ctx);

            // then
            assertThat(result).isInstanceOf(PaymentResult.MaxRetryExceeded.class);
            assertThat(result.funding().getRetryCnt()).isEqualTo(3);

            verify(eventPublisher).publishOrderCancelled(
                    eq(ctx.orderId()),
                    eq(CancelReason.MAX_RETRY_EXCEEDED.name())
            );
            verify(walletService, never()).decrease(any(), any());
            verify(fundingPaymentRepository, never()).save(any(FundingPayment.class));
        }

        @Test
        void RESERVED_타입_신규_결제도_정상_처리된다() {
            // given
            SettlementItem item = new SettlementItem(1L, 1L, 10000L);
            PaymentContext ctx = PaymentContext.forReserved(item, 100L, 1L);

            Wallet updatedWallet = Wallet.builder().id(1L).memberId(1L).balance(20000L).build();

            given(fundingPaymentRepository.findByOrderId(ctx.orderId())).willReturn(Optional.empty());
            given(walletService.decrease(ctx.memberId(), ctx.amount())).willReturn(updatedWallet);
            given(fundingPaymentRepository.save(any(FundingPayment.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            PaymentResult result = processor.process(ctx);

            // then
            assertThat(result).isInstanceOf(PaymentResult.Success.class);
            assertThat(result.funding().getPaymentType()).isEqualTo(PaymentType.RESERVED);
            assertThat(result.funding().getStatus()).isEqualTo(FundingPaymentStatus.SUCCESS);

            verify(walletService).decrease(ctx.memberId(), ctx.amount());
            verify(paymentHistoryRepository).save(any(PaymentHistory.class));
        }
    }
}