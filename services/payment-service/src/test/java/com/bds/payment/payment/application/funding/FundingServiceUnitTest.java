package com.bds.payment.payment.application.funding;

import com.bds.payment.payment.application.wallet.WalletService;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FundingServiceUnitTest {

    @Mock private WalletService walletService;
    @Mock private FundingPaymentProcessor paymentProcessor;
    @Mock private FundingSettlementProcessor settlementProcessor;
    @Mock private FundingEventPublisher eventPublisher;

    @InjectMocks private FundingService fundingService;

    @Nested
    @DisplayName("funding()")
    class FundingTest {

        private FundingPaymentRequestDto dto;
        private FundingPayment funding;

        @BeforeEach
        void setUp() {
            dto = new FundingPaymentRequestDto(1L, 1L, 100L, 10000L, PaymentType.INSTANT);
            funding = FundingPayment.create(dto, 1L, UuidCreator.getTimeOrderedEpoch());

            given(walletService.getWalletId(dto.memberId())).willReturn(1L);
        }

        @Test
        void 결제_성공시_OrderPaid_이벤트를_발행한다() {
            // given
            funding.markSuccess();
            given(paymentProcessor.process(any(PaymentContext.class))).willReturn(new PaymentResult.Success(funding));

            // when
            FundingPaymentResponseDto result = fundingService.funding(dto);

            // then
            assertNotNull(result);
            verify(paymentProcessor).process(any(PaymentContext.class));
            verify(eventPublisher).publishOrderPaid(dto.orderId());
        }

        @Test
        void 이미_결제된_주문이면_멱등_응답만_리턴하고_OrderPaid_이벤트는_발행하지_않는다() {
            // given
            funding.markSuccess();
            given(paymentProcessor.process(any(PaymentContext.class))).willReturn(new PaymentResult.AlreadyPaid(funding));

            // when
            FundingPaymentResponseDto result = fundingService.funding(dto);

            // then
            assertNotNull(result);
            verify(eventPublisher, never()).publishOrderPaid(any());
        }
    }

    @Nested
    @DisplayName("refund()")
    class RefundTest {

        @Test
        void 환불_요청은_SettlementProcessor에_위임하고_이벤트를_발행한다() {
            // given
            RefundRequestDto dto = new RefundRequestDto(
                    UuidCreator.getTimeOrderedEpoch(),
                    1L,
                    1L,
                    1L,
                    10000L,
                    "USER_CANCEL"
            );

            // when
            fundingService.refund(dto);

            // then
            verify(settlementProcessor).processRefundItem(dto.orderId(), dto.memberId(), dto.cancelReason());
            verify(eventPublisher).publishOrderProcessRefunded(List.of(dto.orderId()));
        }
    }

    @Nested
    @DisplayName("confirmSettlement()")
    class ConfirmSettlementTest {

        @Test
        void 모든_항목_성공시_창작자_크레딧을_호출하고_확정_이벤트를_발행한다() {
            // given
            SettlementBatchRequestDto dto = createBatchDto(3);
            given(settlementProcessor.processSettlementItem(any())).willReturn(10000L);

            // when
            SettlementResultResponseDto result = fundingService.confirmSettlement(dto);

            // then
            assertEquals(3, result.successItems().size());
            assertEquals(0, result.failedItems().size());
            verify(settlementProcessor, times(3)).processSettlementItem(any());
            verify(settlementProcessor).creditCreatorForBatch(eq(dto.creatorMemberId()), eq(dto.productId()), anyString());
            verify(eventPublisher).publishOrderProcessConfirmed(anyList());
            verify(eventPublisher, never()).publishOrderCancelled(any(), anyString());
        }

        @Test
        void 멱등성_스킵된_항목은_ALREADY_CONFIRMED로_표시한다() {
            // given
            SettlementBatchRequestDto dto = createBatchDto(2);
            given(settlementProcessor.processSettlementItem(any())).willReturn(0L);

            // when
            SettlementResultResponseDto result = fundingService.confirmSettlement(dto);

            // then
            assertEquals(2, result.successItems().size());
            assertTrue(result.successItems().stream().allMatch(item -> "ALREADY_CONFIRMED".equals(item.message())));
            verify(settlementProcessor).creditCreatorForBatch(any(), any(), anyString());
            verify(eventPublisher).publishOrderProcessConfirmed(anyList());
        }

        private SettlementBatchRequestDto createBatchDto(int itemCount) {
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

    @Nested
    @DisplayName("confirmReservedFunding()")
    class ConfirmReservedFundingTest {

        private SettlementBatchRequestDto dto;
        private FundingPayment successFunding;

        @BeforeEach
        void setUp() {
            dto = createBatchDto(3);

            FundingPaymentRequestDto reqDto = new FundingPaymentRequestDto(
                    1L, 1L, 100L, 10000L, PaymentType.RESERVED
            );
            successFunding = FundingPayment.create(reqDto, 1L, UuidCreator.getTimeOrderedEpoch());
            successFunding.markSuccess();

            given(walletService.getWalletId(any())).willReturn(1L);
        }

        @Test
        void 모든_항목_결제_및_정산_성공시_창작자_크레딧을_호출한다() {
            // given
            given(paymentProcessor.process(any(PaymentContext.class))).willReturn(new PaymentResult.Success(successFunding));
            given(settlementProcessor.processSettlementItem(any())).willReturn(10000L);

            // when
            SettlementResultResponseDto result = fundingService.confirmReservedFunding(dto);

            // then
            assertEquals(3, result.successItems().size());
            assertEquals(0, result.failedItems().size());
            verify(paymentProcessor, times(3)).process(any(PaymentContext.class));
            verify(settlementProcessor, times(3)).processSettlementItem(any());
            verify(settlementProcessor).creditCreatorForBatch(eq(dto.creatorMemberId()), eq(dto.productId()), anyString());
            verify(eventPublisher).publishOrderProcessConfirmed(anyList());
        }

        private SettlementBatchRequestDto createBatchDto(int itemCount) {
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

    @Nested
    @DisplayName("refundFailedFunding()")
    class RefundFailedFundingTest {

        @Test
        void 모든_항목을_Processor에_위임하고_성공_처리한다() {
            // given
            SettlementBatchRequestDto dto = createBatchDto(3);

            // when
            SettlementResultResponseDto result = fundingService.refundFailedFunding(dto);

            // then
            assertEquals(3, result.successItems().size());
            assertEquals(0, result.failedItems().size());
            verify(settlementProcessor, times(3)).processRefundItem(any(), any(), eq("FUNDING_FAILED"));
            verify(eventPublisher).publishOrderProcessRefunded(anyList());
        }

        @Test
        void 이미_환불된_항목은_ALREADY_REFUNDED로_표시한다() {
            // given
            SettlementBatchRequestDto dto = createBatchDto(2);
            doThrow(new BusinessException(ErrorCode.FUNDING_ALREADY_REFUNDED)).when(settlementProcessor).processRefundItem(any(), any(), anyString());

            // when
            SettlementResultResponseDto result = fundingService.refundFailedFunding(dto);

            // then
            assertEquals(2, result.successItems().size());
            assertEquals(0, result.failedItems().size());
            assertTrue(result.successItems().stream().allMatch(item -> "ALREADY_REFUNDED".equals(item.message())));
            verify(eventPublisher, never()).publishOrderProcessRefunded(anyList());
        }

        private SettlementBatchRequestDto createBatchDto(int itemCount) {
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
}