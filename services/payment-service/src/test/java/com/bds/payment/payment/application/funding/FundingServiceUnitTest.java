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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FundingServiceUnitTest {

    @Mock private FundingPaymentRepository fundingPaymentRepository;
    @Mock private PaymentHistoryRepository paymentHistoryRepository;
    @Mock private WalletService walletService;
    @Mock private FundingSettlementProcessor processor;

    @InjectMocks private FundingService fundingService;

    @Nested
    @DisplayName("펀딩 결제")
    class FundingTest {

        @Test
        void INSTANT_결제를_정상적으로_처리한다() {
            // given
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(
                    1L, 1L, 100L, 10000L, PaymentType.INSTANT
            );
            FundingPayment fundingPayment = FundingPayment.create(dto, 1L, UuidCreator.getTimeOrderedEpoch());
            Wallet wallet = Wallet.builder()
                    .id(1L)
                    .memberId(1L)
                    .balance(40000L)
                    .build();

            given(fundingPaymentRepository.existsByOrderId(dto.orderId())).willReturn(false);
            given(walletService.getWalletId(dto.memberId())).willReturn(1L);
            given(walletService.decrease(dto.memberId(), dto.amount())).willReturn(wallet);
            given(fundingPaymentRepository.save(any(FundingPayment.class))).willReturn(fundingPayment);

            // when
            FundingPaymentResponseDto result = fundingService.funding(dto);

            // then
            assertNotNull(result);
            verify(walletService).decrease(dto.memberId(), dto.amount());
            verify(fundingPaymentRepository).save(any(FundingPayment.class));
            verify(paymentHistoryRepository).save(any());
        }

        @Test
        void RESERVED_결제는_지갑_차감_없이_처리한다() {
            // given
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(
                    1L, 1L, 100L, 10000L, PaymentType.RESERVED
            );
            FundingPayment fundingPayment = FundingPayment.create(dto, 1L, UuidCreator.getTimeOrderedEpoch());

            given(fundingPaymentRepository.existsByOrderId(dto.orderId())).willReturn(false);
            given(walletService.getWalletId(dto.memberId())).willReturn(1L);
            given(fundingPaymentRepository.save(any(FundingPayment.class))).willReturn(fundingPayment);

            // when
            FundingPaymentResponseDto result = fundingService.funding(dto);

            // then
            assertNotNull(result);
            verify(walletService, never()).decrease(any(), any());
            verify(paymentHistoryRepository, never()).save(any());
            verify(fundingPaymentRepository).save(any(FundingPayment.class));
        }
    }

    @Nested
    @DisplayName("환불")
    class RefundTest {

        @Test
        void 환불_요청은_Processor에_위임한다() {
            // given
            RefundRequestDto dto = new RefundRequestDto(
                    UuidCreator.getTimeOrderedEpoch(),
                    1L,
                    1L,
                    10000L,
                    "USER_CANCEL"
            );

            // when
            fundingService.refund(dto);

            // then
            verify(processor).processRefundItem(dto.orderId(), dto.memberId(), dto.cancelReason());
        }
    }

    @Nested
    @DisplayName("정산확정 배치")
    class ConfirmSettlementTest {

        @Test
        void 모든_항목이_성공하면_창작자_크레딧을_호출한다() {
            // given
            SettlementBatchRequestDto dto = createBatchDto(3);
            given(processor.processSettlementItem(any())).willReturn(10000L);

            // when
            SettlementResultResponseDto result = fundingService.confirmSettlement(dto);

            // then
            assertEquals(3, result.successItems().size());
            assertEquals(0, result.failedItems().size());
            verify(processor, times(3)).processSettlementItem(any());
            verify(processor).creditCreatorForBatch(eq(dto.creatorMemberId()), eq(dto.productId()), anyString());
        }

        @Test
        void 멱등성_스킵된_항목은_ALREADY_CONFIRMED로_표시한다() {
            // given
            SettlementBatchRequestDto dto = createBatchDto(2);
            given(processor.processSettlementItem(any())).willReturn(0L);

            // when
            SettlementResultResponseDto result = fundingService.confirmSettlement(dto);

            // then
            assertEquals(2, result.successItems().size());
            assertTrue(result.successItems().stream()
                    .allMatch(item -> "ALREADY_CONFIRMED".equals(item.message())));
            verify(processor).creditCreatorForBatch(any(), any(), anyString());
        }

        @Test
        void 일부_항목이_실패해도_나머지는_처리한다() {
            // given
            SettlementBatchRequestDto dto = createBatchDto(3);
            given(processor.processSettlementItem(any()))
                    .willReturn(10000L)
                    .willThrow(new RuntimeException("DB 오류"))
                    .willReturn(10000L);

            // when
            SettlementResultResponseDto result = fundingService.confirmSettlement(dto);

            // then
            assertEquals(2, result.successItems().size());
            assertEquals(1, result.failedItems().size());
            verify(processor).creditCreatorForBatch(any(), any(), anyString());
        }

        @Test
        void 창작자_크레딧_실패시_예외를_전파한다() {
            // given
            SettlementBatchRequestDto dto = createBatchDto(2);
            given(processor.processSettlementItem(any())).willReturn(10000L);
            doThrow(new RuntimeException("지갑 없음"))
                    .when(processor).creditCreatorForBatch(any(), any(), anyString());

            // when & then
            assertThrows(RuntimeException.class, () -> fundingService.confirmSettlement(dto));
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
    @DisplayName("예약펀딩확정 배치")
    class ConfirmReservedFundingTest {

        @Test
        void 모든_항목이_성공하면_창작자_크레딧을_호출한다() {
            // given
            SettlementBatchRequestDto dto = createBatchDto(3);
            given(processor.processReservedFundingItem(any(), any())).willReturn(10000L);

            // when
            SettlementResultResponseDto result = fundingService.confirmReservedFunding(dto);

            // then
            assertEquals(3, result.successItems().size());
            verify(processor, times(3)).processReservedFundingItem(any(), eq(dto.productId()));
            verify(processor).creditCreatorForBatch(eq(dto.creatorMemberId()), eq(dto.productId()), anyString());
        }

        @Test
        void 일부_실패해도_성공한_건은_크레딧에_포함된다() {
            // given
            SettlementBatchRequestDto dto = createBatchDto(3);
            given(processor.processReservedFundingItem(any(), any()))
                    .willReturn(10000L)
                    .willThrow(new RuntimeException("잔액 부족"))
                    .willReturn(10000L);

            // when
            SettlementResultResponseDto result = fundingService.confirmReservedFunding(dto);

            // then
            assertEquals(2, result.successItems().size());
            assertEquals(1, result.failedItems().size());
            verify(processor).creditCreatorForBatch(any(), any(), anyString());
        }

        @Test
        void 멱등성_스킵된_항목은_ALREADY_CONFIRMED로_표시한다() {
            // given
            SettlementBatchRequestDto dto = createBatchDto(2);
            given(processor.processReservedFundingItem(any(), any())).willReturn(0L);

            // when
            SettlementResultResponseDto result = fundingService.confirmReservedFunding(dto);

            // then
            assertThat(result.successItems()).hasSize(2);
            assertThat(result.successItems())
                    .allMatch(item -> "ALREADY_CONFIRMED".equals(item.message()));
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
    @DisplayName("펀딩실패 환불 배치")
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
            verify(processor, times(3)).processRefundItem(any(), any(), eq("FUNDING_FAILED"));
        }

        @Test
        void 이미_환불된_항목은_ALREADY_REFUNDED로_표시한다() {
            // given
            SettlementBatchRequestDto dto = createBatchDto(2);
            doThrow(new BusinessException(ErrorCode.FUNDING_ALREADY_REFUNDED))
                    .when(processor).processRefundItem(any(), any(), anyString());

            // when
            SettlementResultResponseDto result = fundingService.refundFailedFunding(dto);

            // then
            assertEquals(2, result.successItems().size());
            assertEquals(0, result.failedItems().size());
            assertTrue(result.successItems().stream()
                    .allMatch(item -> "ALREADY_REFUNDED".equals(item.message())));
        }

        @Test
        void 예상치_못한_예외는_failedItems에_담긴다() {
            // given
            SettlementBatchRequestDto dto = createBatchDto(2);
            doThrow(new RuntimeException("DB 오류"))
                    .when(processor).processRefundItem(any(), any(), anyString());

            // when
            SettlementResultResponseDto result = fundingService.refundFailedFunding(dto);

            // then
            assertEquals(0, result.successItems().size());
            assertEquals(2, result.failedItems().size());
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