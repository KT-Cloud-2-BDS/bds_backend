package com.bds.payment.payment.application.funding;

import com.bds.payment.payment.application.wallet.WalletService;
import com.bds.payment.payment.domain.common.CancelReason;
import com.bds.payment.payment.domain.common.PaymentType;
import com.bds.payment.payment.domain.fundingPayment.FundingPayment;
import com.bds.payment.payment.global.exception.BusinessException;
import com.bds.payment.payment.global.exception.ErrorCode;
import com.bds.payment.payment.presentation.request.FundingPaymentRequestDto;
import com.bds.payment.payment.presentation.request.RefundRequestDto;
import com.bds.payment.payment.presentation.request.SettlementBatchRequestDto;
import com.bds.payment.payment.presentation.request.SettlementBatchRequestDto.SettlementItem;
import com.bds.payment.payment.presentation.response.FundingPaymentResponseDto;
import com.bds.payment.payment.presentation.response.SettlementResultResponseDto;
import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FundingServiceUnitExceptionTest {

    @Mock private WalletService walletService;
    @Mock private FundingPaymentProcessor paymentProcessor;
    @Mock private FundingSettlementProcessor settlementProcessor;
    @Mock private FundingEventPublisher eventPublisher;
    @Mock private FundingFailureHandler failureHandler;

    @InjectMocks private FundingService fundingService;

    @Nested
    @DisplayName("funding() 예외")
    class FundingExceptionTest {

        @Test
        void 유저_귀책_실패시_OrderPaid_이벤트를_발행하지_않는다() {
            // given
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(
                    1L, 1L, 100L, 10000L, PaymentType.INSTANT
            );
            FundingPayment funding = FundingPayment.create(dto, 1L, UuidCreator.getTimeOrderedEpoch());
            funding.markFailed();

            given(walletService.getWalletId(dto.memberId())).willReturn(1L);
            given(paymentProcessor.process(any(PaymentContext.class))).willReturn(new PaymentResult.UserFailure(funding));

            // when
            FundingPaymentResponseDto result = fundingService.funding(dto);

            // then
            assertNotNull(result);
            verify(eventPublisher, never()).publishOrderPaid(any());
        }

        @Test
        void 재시도_초과시_OrderPaid_이벤트를_발행하지_않는다() {
            // given
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(
                    1L, 1L, 100L, 10000L, PaymentType.INSTANT
            );
            FundingPayment funding = FundingPayment.create(dto, 1L, UuidCreator.getTimeOrderedEpoch());
            for (int i = 0; i < FundingPayment.MAX_RETRY; i++) {
                funding.markFailed();
            }

            given(walletService.getWalletId(dto.memberId())).willReturn(1L);
            given(paymentProcessor.process(any(PaymentContext.class))).willReturn(new PaymentResult.MaxRetryExceeded(funding));

            // when
            FundingPaymentResponseDto result = fundingService.funding(dto);

            // then
            assertNotNull(result);
            verify(eventPublisher, never()).publishOrderPaid(any());
        }

        @Test
        void Processor에서_시스템_예외_발생시_그대로_전파한다() {
            // given
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(
                    1L, 1L, 100L, 10000L, PaymentType.INSTANT
            );

            given(walletService.getWalletId(dto.memberId())).willReturn(1L);
            given(paymentProcessor.process(any(PaymentContext.class))).willThrow(new BusinessException(ErrorCode.PAYMENT_SERVER_ERROR));

            // when & then
            assertThatThrownBy(() -> fundingService.funding(dto))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_SERVER_ERROR);
                    });
            verify(failureHandler).handleSystemError(dto.orderId());
            verify(eventPublisher, never()).publishOrderPaid(any());
        }
    }

    @Nested
    @DisplayName("refund() 예외")
    class RefundExceptionTest {

        @Test
        void Processor에서_예외_발생시_그대로_전파한다() {
            // given
            RefundRequestDto dto = new RefundRequestDto(
                    UuidCreator.getTimeOrderedEpoch(),
                    1L,
                    1L,
                    1L,
                    10000L,
                    "USER_CANCEL"
            );
            doThrow(new BusinessException(ErrorCode.FUNDING_NOT_FOUND)).when(settlementProcessor).processRefundItem(dto.orderId(), dto.memberId(), dto.cancelReason());

            // when & then
            assertThatThrownBy(() -> fundingService.refund(dto))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FUNDING_NOT_FOUND);
                    });
            verify(eventPublisher, never()).publishOrderProcessRefunded(any());
        }
    }

    @Nested
    @DisplayName("confirmSettlement() 예외")
    class ConfirmSettlementExceptionTest {

        @Test
        void 일부_항목_실패시_PAYMENT_SERVER_ERROR_이벤트를_발행한다() {
            // given
            SettlementBatchRequestDto dto = createBatchDto(3);
            given(settlementProcessor.processSettlementItem(any()))
                    .willReturn(10000L)
                    .willThrow(new RuntimeException("DB 오류"))
                    .willReturn(10000L);

            // when
            SettlementResultResponseDto result = fundingService.confirmSettlement(dto);

            // then
            assertEquals(2, result.successItems().size());
            assertEquals(1, result.failedItems().size());
            verify(eventPublisher).publishOrderCancelled(any(), eq(CancelReason.PAYMENT_SERVER_ERROR.name()));
            verify(eventPublisher).publishOrderProcessConfirmed(anyList());
        }

        @Test
        void 창작자_크레딧_실패는_예외를_전파한다() {
            // given
            SettlementBatchRequestDto dto = createBatchDto(2);
            given(settlementProcessor.processSettlementItem(any())).willReturn(10000L);
            doThrow(new BusinessException(ErrorCode.WALLET_NOT_FOUND)).when(settlementProcessor).creditCreatorForBatch(any(), any(), anyString());

            // when & then
            assertThatThrownBy(() -> fundingService.confirmSettlement(dto))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("confirmReservedFunding() 예외")
    class ConfirmReservedFundingExceptionTest {

        @Test
        void 결제_실패한_항목은_failedItems에_담긴다() {
            // given
            SettlementBatchRequestDto dto = createBatchDto(3);

            FundingPayment successFunding = createSuccessFunding();
            FundingPayment failedFunding = createFailedFunding();

            given(walletService.getWalletId(any())).willReturn(1L);
            given(paymentProcessor.process(any(PaymentContext.class)))
                    .willReturn(new PaymentResult.Success(successFunding))
                    .willReturn(new PaymentResult.UserFailure(failedFunding))
                    .willReturn(new PaymentResult.Success(successFunding));
            given(settlementProcessor.processSettlementItem(any())).willReturn(10000L);

            // when
            SettlementResultResponseDto result = fundingService.confirmReservedFunding(dto);

            // then
            assertEquals(2, result.successItems().size());
            assertEquals(1, result.failedItems().size());
        }

        @Test
        void 재시도_초과_항목도_failedItems에_담긴다() {
            // given
            SettlementBatchRequestDto dto = createBatchDto(2);
            FundingPayment maxRetryFunding = createFailedFunding();
            for (int i = 0; i < FundingPayment.MAX_RETRY - 1; i++) {
                maxRetryFunding.markFailed();
            }

            given(walletService.getWalletId(any())).willReturn(1L);
            given(paymentProcessor.process(any(PaymentContext.class))).willReturn(new PaymentResult.MaxRetryExceeded(maxRetryFunding));

            // when
            SettlementResultResponseDto result = fundingService.confirmReservedFunding(dto);

            // then
            assertEquals(0, result.successItems().size());
            assertEquals(2, result.failedItems().size());
            verify(settlementProcessor, never()).processSettlementItem(any());
        }

        @Test
        void 창작자_크레딧_실패는_예외를_전파한다() {
            // given
            SettlementBatchRequestDto dto = createBatchDto(2);
            FundingPayment successFunding = createSuccessFunding();

            given(walletService.getWalletId(any())).willReturn(1L);
            given(paymentProcessor.process(any(PaymentContext.class))).willReturn(new PaymentResult.Success(successFunding));
            given(settlementProcessor.processSettlementItem(any())).willReturn(10000L);
            doThrow(new BusinessException(ErrorCode.WALLET_NOT_FOUND)).when(settlementProcessor).creditCreatorForBatch(any(), any(), anyString());

            // when & then
            assertThatThrownBy(() -> fundingService.confirmReservedFunding(dto))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_NOT_FOUND);
                    });
        }

        private FundingPayment createSuccessFunding() {
            FundingPayment fp = FundingPayment.create(
                    new FundingPaymentRequestDto(1L, 1L, 100L, 10000L, PaymentType.RESERVED),
                    1L, UuidCreator.getTimeOrderedEpoch()
            );
            fp.markSuccess();
            return fp;
        }

        private FundingPayment createFailedFunding() {
            FundingPayment fp = FundingPayment.create(
                    new FundingPaymentRequestDto(1L, 1L, 100L, 10000L, PaymentType.RESERVED),
                    1L, UuidCreator.getTimeOrderedEpoch()
            );
            fp.markFailed();
            return fp;
        }
    }

    @Nested
    @DisplayName("refundFailedFunding() 예외")
    class RefundFailedFundingExceptionTest {

        @Test
        void 예상치_못한_예외는_failedItems에_담긴다() {
            // given
            SettlementBatchRequestDto dto = createBatchDto(2);
            doThrow(new RuntimeException("DB 오류")).when(settlementProcessor).processRefundItem(any(), any(), anyString());

            // when
            SettlementResultResponseDto result = fundingService.refundFailedFunding(dto);

            // then
            assertEquals(0, result.successItems().size());
            assertEquals(2, result.failedItems().size());
            verify(eventPublisher, never()).publishOrderProcessRefunded(anyList());
        }

        @Test
        void 모든_항목이_실패해도_예외를_전파하지_않는다() {
            // given
            SettlementBatchRequestDto dto = createBatchDto(3);
            doThrow(new RuntimeException("일시적 오류")).when(settlementProcessor).processRefundItem(any(), any(), anyString());

            // when
            SettlementResultResponseDto result = fundingService.refundFailedFunding(dto);

            // then
            assertNotNull(result);
            assertEquals(3, result.failedItems().size());
        }
    }

    private static SettlementBatchRequestDto createBatchDto(int itemCount) {
        List<SettlementItem> items = new ArrayList<>();
        for (int i = 1; i <= itemCount; i++) {
            items.add(new SettlementItem((long) i, (long) i, 10000L));
        }
        return new SettlementBatchRequestDto(
                UuidCreator.getTimeOrderedEpoch(),
                null,
                999L,
                100L,
                items
        );
    }
}