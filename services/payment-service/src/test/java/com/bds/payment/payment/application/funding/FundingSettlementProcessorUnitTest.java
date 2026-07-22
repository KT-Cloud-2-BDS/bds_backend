package com.bds.payment.payment.application.funding;

import com.bds.payment.payment.application.wallet.WalletService;
import com.bds.payment.payment.domain.common.FundingPaymentStatus;
import com.bds.payment.payment.domain.common.PaymentType;
import com.bds.payment.payment.domain.fundingPayment.FundingPayment;
import com.bds.payment.payment.domain.fundingPayment.FundingPaymentRepository;
import com.bds.payment.payment.domain.paymentHistory.PaymentHistoryRepository;
import com.bds.payment.payment.domain.wallet.Wallet;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FundingSettlementProcessorUnitTest {

    @Mock private FundingPaymentRepository fundingPaymentRepository;
    @Mock private PaymentHistoryRepository paymentHistoryRepository;
    @Mock private WalletService walletService;

    @InjectMocks
    private FundingSettlementProcessor processor;

    @Nested
    @DisplayName("정산확정 단건 처리")
    class ProcessSettlementItemTest {

        @Test
        void SUCCESS_상태를_CONFIRMED로_변경하고_금액을_반환한다() {
            // given
            SettlementItem item = new SettlementItem(1L, 1L, 10000L);
            FundingPayment fp = FundingPayment.builder()
                    .orderId(1L)
                    .walletId(1L)
                    .productId(100L)
                    .amount(10000L)
                    .paymentType(PaymentType.INSTANT)
                    .status(FundingPaymentStatus.SUCCESS)
                    .build();
            given(fundingPaymentRepository.findByOrderId(item.orderId())).willReturn(Optional.of(fp));

            // when
            long result = processor.processSettlementItem(item);

            // then
            assertThat(result).isEqualTo(10000L);
            assertThat(fp.getStatus()).isEqualTo(FundingPaymentStatus.CONFIRMED);
            verify(fundingPaymentRepository).save(fp);
        }

        @Test
        void 이미_CONFIRMED된_항목은_0을_반환한다() {
            // given (멱등성 스킵)
            SettlementItem item = new SettlementItem(1L, 1L, 10000L);
            FundingPayment fp = FundingPayment.builder()
                    .orderId(1L)
                    .amount(10000L)
                    .status(FundingPaymentStatus.CONFIRMED)
                    .build();
            given(fundingPaymentRepository.findByOrderId(item.orderId())).willReturn(Optional.of(fp));

            // when
            long result = processor.processSettlementItem(item);

            // then
            assertThat(result).isEqualTo(0L);
            verify(fundingPaymentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("예약펀딩확정 단건 처리")
    class ProcessReservedFundingItemTest {

        @Test
        void 신규_funding_payment를_생성하고_지갑에서_차감한다() {
            // given
            SettlementItem item = new SettlementItem(1L, 1L, 10000L);
            Long productId = 100L;
            Wallet buyerWallet = Wallet.builder()
                    .id(1L)
                    .memberId(1L)
                    .balance(20000L)
                    .build();
            FundingPayment savedFp = FundingPayment.builder()
                    .id(1L)
                    .orderId(1L)
                    .walletId(1L)
                    .productId(productId)
                    .amount(10000L)
                    .paymentType(PaymentType.RESERVED)
                    .status(FundingPaymentStatus.CONFIRMED)
                    .build();

            given(fundingPaymentRepository.existsByOrderId(item.orderId())).willReturn(false);
            given(walletService.decrease(item.memberId(), item.amount())).willReturn(buyerWallet);
            given(fundingPaymentRepository.save(any(FundingPayment.class))).willReturn(savedFp);

            // when
            long result = processor.processReservedFundingItem(item, productId);

            // then
            assertThat(result).isEqualTo(10000L);
            verify(walletService).decrease(item.memberId(), item.amount());
            verify(fundingPaymentRepository).save(any(FundingPayment.class));
            verify(paymentHistoryRepository).save(any());
        }

        @Test
        void 이미_존재하는_orderId면_0을_반환한다() {
            // given (멱등성 스킵)
            SettlementItem item = new SettlementItem(1L, 1L, 10000L);
            given(fundingPaymentRepository.existsByOrderId(item.orderId())).willReturn(true);

            // when
            long result = processor.processReservedFundingItem(item, 100L);

            // then
            assertThat(result).isEqualTo(0L);
            verify(walletService, never()).decrease(any(), any());
            verify(fundingPaymentRepository, never()).save(any(FundingPayment.class));
            verify(paymentHistoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("환불 단건 처리")
    class ProcessRefundItemTest {

        @Test
        void INSTANT_환불시_지갑에_금액을_충전한다() {
            // given
            Long orderId = 1L;
            Long memberId = 1L;
            Long walletId = 1L;
            FundingPayment fp = FundingPayment.builder()
                    .orderId(orderId)
                    .walletId(walletId)
                    .amount(10000L)
                    .paymentType(PaymentType.INSTANT)
                    .status(FundingPaymentStatus.SUCCESS)
                    .build();
            Wallet wallet = Wallet.builder()
                    .id(walletId)
                    .memberId(memberId)
                    .balance(30000L)
                    .build();

            given(fundingPaymentRepository.findByOrderId(orderId)).willReturn(Optional.of(fp));
            given(walletService.getWalletId(memberId)).willReturn(walletId);
            given(walletService.charge(memberId, fp.getAmount())).willReturn(wallet);

            // when
            processor.processRefundItem(orderId, memberId, "USER_CANCEL");

            // then
            assertThat(fp.getStatus()).isEqualTo(FundingPaymentStatus.REFUNDED);
            verify(walletService).charge(memberId, fp.getAmount());
            verify(paymentHistoryRepository).save(any());
            verify(fundingPaymentRepository).save(fp);
        }

        @Test
        void RESERVED_환불시_지갑_충전_없이_상태만_변경한다() {
            // given
            Long orderId = 1L;
            Long memberId = 1L;
            Long walletId = 1L;
            FundingPayment fp = FundingPayment.builder()
                    .orderId(orderId)
                    .walletId(walletId)
                    .amount(10000L)
                    .paymentType(PaymentType.RESERVED)
                    .status(FundingPaymentStatus.SUCCESS)
                    .build();

            given(fundingPaymentRepository.findByOrderId(orderId)).willReturn(Optional.of(fp));
            given(walletService.getWalletId(memberId)).willReturn(walletId);

            // when
            processor.processRefundItem(orderId, memberId, "USER_CANCEL");

            // then
            assertThat(fp.getStatus()).isEqualTo(FundingPaymentStatus.REFUNDED);
            verify(walletService, never()).charge(any(), any());
            verify(paymentHistoryRepository, never()).save(any());
            verify(fundingPaymentRepository).save(fp);
        }
    }

    @Nested
    @DisplayName("창작자 크레딧 배치 처리")
    class CreditCreatorForBatchTest {

        @Test
        void 미크레딧_항목의_금액을_합산하여_창작자_지갑에_충전한다() {
            // given
            Long creatorId = 999L;
            Long productId = 100L;
            List<FundingPayment> uncredited = List.of(
                    FundingPayment.builder().id(1L).amount(10000L).status(FundingPaymentStatus.CONFIRMED).build(),
                    FundingPayment.builder().id(2L).amount(20000L).status(FundingPaymentStatus.CONFIRMED).build(),
                    FundingPayment.builder().id(3L).amount(30000L).status(FundingPaymentStatus.CONFIRMED).build()
            );
            Wallet creatorWallet = Wallet.builder()
                    .id(999L)
                    .memberId(creatorId)
                    .balance(60000L)
                    .build();

            given(fundingPaymentRepository.findUncreditedForUpdate(productId, FundingPaymentStatus.CONFIRMED))
                    .willReturn(uncredited);
            given(walletService.charge(creatorId, 60000L)).willReturn(creatorWallet);

            // when
            processor.creditCreatorForBatch(creatorId, productId, "정산 확정");

            // then
            verify(walletService).charge(creatorId, 60000L);  // 10000 + 20000 + 30000
            verify(fundingPaymentRepository).saveAll(uncredited);
            verify(paymentHistoryRepository).save(any());

            // 모든 항목에 creditedAt 마킹됨
            assertThat(uncredited).allMatch(fp -> fp.getCreditedAt() != null);
        }

        @Test
        void 미크레딧_항목이_없으면_아무_동작도_하지_않는다() {
            // given (재시도 시나리오)
            Long creatorId = 999L;
            Long productId = 100L;
            given(fundingPaymentRepository.findUncreditedForUpdate(productId, FundingPaymentStatus.CONFIRMED))
                    .willReturn(List.of());

            // when
            processor.creditCreatorForBatch(creatorId, productId, "정산 확정");

            // then
            verify(walletService, never()).charge(any(), any());
            verify(fundingPaymentRepository, never()).saveAll(any());
            verify(paymentHistoryRepository, never()).save(any());
        }
    }
}