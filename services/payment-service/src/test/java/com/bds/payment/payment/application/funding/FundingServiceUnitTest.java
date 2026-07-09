package com.bds.payment.payment.application.funding;

import com.bds.payment.payment.application.wallet.WalletService;
import com.bds.payment.payment.domain.common.FundingPaymentStatus;
import com.bds.payment.payment.domain.common.PaymentType;
import com.bds.payment.payment.domain.fundingPayment.FundingPayment;
import com.bds.payment.payment.domain.fundingPayment.FundingPaymentRepository;
import com.bds.payment.payment.domain.wallet.Wallet;
import com.bds.payment.payment.presentation.request.FundingPaymentRequestDto;
import com.bds.payment.payment.presentation.response.FundingPaymentResponseDto;
import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FundingServiceUnitTest {

    @Mock private FundingPaymentRepository fundingPaymentRepository;
    @Mock private WalletService walletService;

    @InjectMocks private FundingService fundingService;

    @Nested
    @DisplayName("펀딩 결제 정상 테스트")
    class fundingTest {

        @Test
        public void INSTANT_결제를_정상적으로_처리한다() {
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
        }

        @Test
        public void RESERVED_결제를_정상적으로_처리한다() {
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
            verify(fundingPaymentRepository).save(any(FundingPayment.class));
        }
    }

    @Nested
    @DisplayName("환불 정상 테스트")
    class refundTest {

        @Test
        public void INSTANT_환불을_정상적으로_처리한다() {
            // given
            Long memberId = 1L;
            Long orderId = 1L;
            FundingPayment fundingPayment = FundingPayment.builder()
                    .orderId(orderId)
                    .walletId(1L)
                    .amount(10000L)
                    .paymentType(PaymentType.INSTANT)
                    .status(FundingPaymentStatus.SUCCESS)
                    .build();
            given(fundingPaymentRepository.findByOrderId(orderId)).willReturn(Optional.of(fundingPayment));

            // when
            fundingService.refund(memberId, orderId);

            // then
            verify(walletService).charge(memberId, fundingPayment.getAmount());
            verify(fundingPaymentRepository).save(fundingPayment);
        }

        @Test
        public void RESERVED_환불을_정상적으로_처리한다() {
            // given
            Long memberId = 1L;
            Long orderId = 1L;
            FundingPayment fundingPayment = FundingPayment.builder()
                    .orderId(orderId)
                    .walletId(1L)
                    .amount(10000L)
                    .paymentType(PaymentType.RESERVED)
                    .status(FundingPaymentStatus.SUCCESS)
                    .build();
            given(fundingPaymentRepository.findByOrderId(orderId)).willReturn(Optional.of(fundingPayment));

            // when
            fundingService.refund(memberId, orderId);

            // then
            verify(walletService, never()).charge(any(), any());
            verify(fundingPaymentRepository).save(fundingPayment);
        }
    }
}