package com.bds.payment.payment.application.funding;

import com.bds.payment.payment.application.wallet.WalletService;
import com.bds.payment.payment.domain.common.FundingPaymentStatus;
import com.bds.payment.payment.domain.common.PaymentType;
import com.bds.payment.payment.domain.fundingPayment.FundingPayment;
import com.bds.payment.payment.domain.fundingPayment.FundingPaymentRepository;
import com.bds.payment.payment.domain.paymentHistory.PaymentHistoryRepository;
import com.bds.payment.payment.global.exception.BusinessException;
import com.bds.payment.payment.global.exception.ErrorCode;
import com.bds.payment.payment.presentation.request.SettlementBatchRequestDto.SettlementItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class FundingSettlementProcessorUnitExceptionTest {

    @Mock private FundingPaymentRepository fundingPaymentRepository;
    @Mock private PaymentHistoryRepository paymentHistoryRepository;
    @Mock private WalletService walletService;

    @InjectMocks private FundingSettlementProcessor processor;

    @Nested
    @DisplayName("정산확정 단건 예외")
    class ProcessSettlementItemExceptionTest {

        @Test
        void 존재하지_않는_주문이면_예외를_던진다() {
            // given
            SettlementItem item = new SettlementItem(1L, 1L, 10000L);
            given(fundingPaymentRepository.findByOrderId(item.orderId())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> processor.processSettlementItem(item))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FUNDING_NOT_FOUND);
                    });
            verify(fundingPaymentRepository, never()).save(any());
        }

        @Test
        void 요청_금액이_실제_금액과_다르면_예외를_던진다() {
            // given
            SettlementItem item = new SettlementItem(1L, 1L, 99999L);
            FundingPayment fp = FundingPayment.builder()
                    .orderId(1L)
                    .amount(10000L)  // 실제는 10000
                    .status(FundingPaymentStatus.SUCCESS)
                    .build();
            given(fundingPaymentRepository.findByOrderId(item.orderId())).willReturn(Optional.of(fp));

            // when & then
            assertThatThrownBy(() -> processor.processSettlementItem(item))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SETTLEMENT_AMOUNT_MISMATCH);
                    });
            verify(fundingPaymentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("예약펀딩확정 단건 예외")
    class ProcessReservedFundingItemExceptionTest {

        @Test
        void 잔액이_부족하면_예외를_던진다() {
            // given
            SettlementItem item = new SettlementItem(1L, 1L, 10000L);
            given(fundingPaymentRepository.existsByOrderId(item.orderId())).willReturn(false);
            given(walletService.decrease(item.memberId(), item.amount()))
                    .willThrow(new BusinessException(ErrorCode.WALLET_INSUFFICIENT_BALANCE));

            // when & then
            assertThatThrownBy(() -> processor.processReservedFundingItem(item, 100L))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_INSUFFICIENT_BALANCE);
                    });
            verify(fundingPaymentRepository, never()).save(any(FundingPayment.class));
            verify(paymentHistoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("환불 단건 예외")
    class ProcessRefundItemExceptionTest {

        @Test
        void 존재하지_않는_주문이면_예외를_던진다() {
            // given
            given(fundingPaymentRepository.findByOrderId(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> processor.processRefundItem(1L, 1L, "USER_CANCEL"))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FUNDING_NOT_FOUND);
                    });
            verify(walletService, never()).charge(any(), any());
            verify(fundingPaymentRepository, never()).save(any(FundingPayment.class));
        }

        @Test
        void 타인의_거래를_환불하면_예외를_던진다() {
            // given
            Long orderId = 1L;
            Long memberId = 1L;
            Long ownerWalletId = 1L;
            Long otherWalletId = 999L;

            FundingPayment fp = FundingPayment.builder()
                    .orderId(orderId)
                    .walletId(ownerWalletId)
                    .amount(10000L)
                    .paymentType(PaymentType.INSTANT)
                    .status(FundingPaymentStatus.SUCCESS)
                    .build();

            given(fundingPaymentRepository.findByOrderId(orderId)).willReturn(Optional.of(fp));
            given(walletService.getWalletId(memberId)).willReturn(otherWalletId);

            // when & then
            assertThatThrownBy(() -> processor.processRefundItem(orderId, memberId, "USER_CANCEL"))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FUNDING_ACCESS_DENIED);
                    });
            verify(walletService, never()).charge(any(), any());
            verify(fundingPaymentRepository, never()).save(any(FundingPayment.class));
        }

        @Test
        void 이미_환불된_거래를_환불하면_예외를_던진다() {
            // given
            Long orderId = 1L;
            Long memberId = 1L;
            Long walletId = 1L;

            FundingPayment fp = FundingPayment.builder()
                    .orderId(orderId)
                    .walletId(walletId)
                    .amount(10000L)
                    .paymentType(PaymentType.INSTANT)
                    .status(FundingPaymentStatus.REFUNDED)
                    .build();

            given(fundingPaymentRepository.findByOrderId(orderId)).willReturn(Optional.of(fp));
            given(walletService.getWalletId(memberId)).willReturn(walletId);

            // when & then
            assertThatThrownBy(() -> processor.processRefundItem(orderId, memberId, "USER_CANCEL"))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FUNDING_ALREADY_REFUNDED);
                    });
            verify(walletService, never()).charge(any(), any());
            verify(fundingPaymentRepository, never()).save(any(FundingPayment.class));
        }

        @Test
        void 환불_불가능한_상태면_예외를_던진다() {
            // given: FAILED 상태
            Long orderId = 1L;
            Long memberId = 1L;
            Long walletId = 1L;

            FundingPayment fp = FundingPayment.builder()
                    .orderId(orderId)
                    .walletId(walletId)
                    .amount(10000L)
                    .paymentType(PaymentType.INSTANT)
                    .status(FundingPaymentStatus.FAILED)  // SUCCESS/CONFIRMED가 아님
                    .build();

            given(fundingPaymentRepository.findByOrderId(orderId)).willReturn(Optional.of(fp));
            given(walletService.getWalletId(memberId)).willReturn(walletId);

            // when & then
            assertThatThrownBy(() -> processor.processRefundItem(orderId, memberId, "USER_CANCEL"))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FUNDING_INVALID_STATUS);
                    });
            verify(walletService, never()).charge(any(), any());
            verify(fundingPaymentRepository, never()).save(any(FundingPayment.class));
        }
    }

    @Nested
    @DisplayName("창작자 크레딧 예외")
    class CreditCreatorForBatchExceptionTest {

        @Test
        void 창작자_지갑이_없으면_예외를_던진다() {
            // given
            Long creatorId = 999L;
            Long productId = 100L;
            List<FundingPayment> uncredited = List.of(
                    FundingPayment.builder().id(1L).amount(10000L).status(FundingPaymentStatus.CONFIRMED).build()
            );

            given(fundingPaymentRepository.findUncreditedForUpdate(productId, FundingPaymentStatus.CONFIRMED))
                    .willReturn(uncredited);
            given(walletService.charge(creatorId, 10000L))
                    .willThrow(new BusinessException(ErrorCode.WALLET_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> processor.creditCreatorForBatch(creatorId, productId, "정산 확정"))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_NOT_FOUND);
                    });
            verify(fundingPaymentRepository, never()).saveAll(any());
            verify(paymentHistoryRepository, never()).save(any());
        }
    }
}