package com.bds.payment.payment.application.funding;

import com.bds.payment.payment.application.wallet.WalletService;
import com.bds.payment.payment.domain.common.PaymentType;
import com.bds.payment.payment.domain.fundingPayment.FundingPayment;
import com.bds.payment.payment.domain.fundingPayment.FundingPaymentRepository;
import com.bds.payment.payment.domain.paymentHistory.PaymentHistoryRepository;
import com.bds.payment.payment.global.exception.BusinessException;
import com.bds.payment.payment.global.exception.ErrorCode;
import com.bds.payment.payment.presentation.request.FundingPaymentRequestDto;
import com.bds.payment.payment.presentation.request.RefundRequestDto;
import com.bds.payment.payment.presentation.request.SettlementBatchRequestDto;
import com.bds.payment.payment.presentation.request.SettlementBatchRequestDto.SettlementItem;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FundingServiceUnitExceptionTest {

    @Mock private FundingPaymentRepository fundingPaymentRepository;
    @Mock private PaymentHistoryRepository paymentHistoryRepository;
    @Mock private WalletService walletService;
    @Mock private FundingSettlementProcessor processor;

    @InjectMocks private FundingService fundingService;

    @Nested
    @DisplayName("펀딩 결제 예외")
    class FundingExceptionTest {

        @Test
        void 중복된_주문이면_예외를_던진다() {
            // given
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(
                    1L, 1L, 100L, 10000L, PaymentType.INSTANT
            );
            given(fundingPaymentRepository.existsByOrderId(dto.orderId())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> fundingService.funding(dto))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FUNDING_DUPLICATED);
                        assertThat(ex.getMessage()).isEqualTo(ErrorCode.FUNDING_DUPLICATED.getMessage());
                    });
            verify(walletService, never()).decrease(any(), any());
            verify(fundingPaymentRepository, never()).save(any(FundingPayment.class));
        }

        @Test
        void INSTANT_잔액이_부족하면_예외를_던진다() {
            // given
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(
                    1L, 1L, 100L, 10000L, PaymentType.INSTANT
            );
            FundingPayment fundingPayment = FundingPayment.create(dto, 1L, UuidCreator.getTimeOrderedEpoch());

            given(fundingPaymentRepository.existsByOrderId(dto.orderId())).willReturn(false);
            given(walletService.getWalletId(dto.memberId())).willReturn(1L);
            given(fundingPaymentRepository.save(any(FundingPayment.class))).willReturn(fundingPayment);
            given(walletService.decrease(dto.memberId(), dto.amount()))
                    .willThrow(new BusinessException(ErrorCode.WALLET_INSUFFICIENT_BALANCE));

            // when & then
            assertThatThrownBy(() -> fundingService.funding(dto))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_INSUFFICIENT_BALANCE);
                    });
            verify(paymentHistoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("환불 위임")
    class RefundDelegationTest {

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
            doThrow(new BusinessException(ErrorCode.FUNDING_NOT_FOUND))
                    .when(processor).processRefundItem(dto.orderId(), dto.memberId(), dto.cancelReason());

            // when & then
            assertThatThrownBy(() -> fundingService.refund(dto))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FUNDING_NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("정산확정 배치 예외")
    class ConfirmSettlementExceptionTest {

        @Test
        void 창작자_크레딧_실패는_예외를_전파한다() {
            // given
            SettlementBatchRequestDto dto = createBatchDto(2);
            given(processor.processSettlementItem(any())).willReturn(10000L);
            doThrow(new BusinessException(ErrorCode.WALLET_NOT_FOUND))
                    .when(processor).creditCreatorForBatch(any(), any(), anyString());

            // when & then
            assertThatThrownBy(() -> fundingService.confirmSettlement(dto))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("예약펀딩확정 배치 예외")
    class ConfirmReservedFundingExceptionTest {

        @Test
        void 창작자_크레딧_실패는_예외를_전파한다() {
            // given
            SettlementBatchRequestDto dto = createBatchDto(2);
            given(processor.processReservedFundingItem(any(), any())).willReturn(10000L);
            doThrow(new BusinessException(ErrorCode.WALLET_NOT_FOUND))
                    .when(processor).creditCreatorForBatch(any(), any(), anyString());

            // when & then
            assertThatThrownBy(() -> fundingService.confirmReservedFunding(dto))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_NOT_FOUND);
                    });
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