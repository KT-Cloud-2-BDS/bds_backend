package com.bds.payment.payment.application.funding;

import com.bds.payment.payment.application.wallet.WalletService;
import com.bds.payment.payment.domain.common.FundingPaymentStatus;
import com.bds.payment.payment.domain.common.PaymentType;
import com.bds.payment.payment.domain.fundingPayment.FundingPayment;
import com.bds.payment.payment.domain.fundingPayment.FundingPaymentRepository;
import com.bds.payment.payment.presentation.request.FundingPaymentRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FundingServiceUnitExceptionTest {

    @Mock private FundingPaymentRepository fundingPaymentRepository;
    @Mock private WalletService walletService;

    @InjectMocks private FundingService fundingService;

    @Nested
    @DisplayName("펀딩 결제 예외 테스트")
    class fundingExceptionTest {

        @Test
        public void 중복된_주문이면_예외를_던진다() {
            // given
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(
                    1L, 1L, 100L, 10000L, PaymentType.INSTANT
            );
            given(fundingPaymentRepository.existsByOrderId(dto.orderId())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> fundingService.funding(dto))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(walletService, never()).decrease(any(), any());
            verify(fundingPaymentRepository, never()).save(any(FundingPayment.class));
        }

        @Test
        public void INSTANT_잔액이_부족하면_예외를_던진다() {
            // given
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(
                    1L, 1L, 100L, 10000L, PaymentType.INSTANT
            );
            given(fundingPaymentRepository.existsByOrderId(dto.orderId())).willReturn(false);
            given(walletService.getWalletId(dto.memberId())).willReturn(1L);
            given(walletService.decrease(dto.memberId(), dto.amount())).willThrow(new IllegalArgumentException("잔액이 부족합니다."));

            // when & then
            assertThatThrownBy(() -> fundingService.funding(dto))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(fundingPaymentRepository, never()).save(any(FundingPayment.class));
        }
    }

    @Nested
    @DisplayName("환불 예외 테스트")
    class refundExceptionTest {

        @Test
        public void 존재하지_않는_거래면_예외를_던진다() {
            // given
            Long memberId = 1L;
            Long orderId = 1L;
            given(fundingPaymentRepository.findByOrderId(orderId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> fundingService.refund(memberId, orderId))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(walletService, never()).charge(any(), any());
        }

        @Test
        public void 이미_환불된_거래면_예외를_던진다() {
            // given
            Long memberId = 1L;
            Long orderId = 1L;
            FundingPayment fundingPayment = FundingPayment.builder()
                    .orderId(orderId)
                    .walletId(1L)
                    .amount(10000L)
                    .paymentType(PaymentType.INSTANT)
                    .status(FundingPaymentStatus.REFUNDED)
                    .build();
            given(fundingPaymentRepository.findByOrderId(orderId)).willReturn(Optional.of(fundingPayment));

            // when & then
            assertThatThrownBy(() -> fundingService.refund(memberId, orderId))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(walletService, never()).charge(any(), any());
            verify(fundingPaymentRepository, never()).save(any(FundingPayment.class));
        }
    }
}