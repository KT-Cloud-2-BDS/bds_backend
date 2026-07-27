package com.bds.payment.payment.application.funding;

import com.bds.payment.payment.application.wallet.WalletService;
import com.bds.payment.payment.domain.common.FundingPaymentStatus;
import com.bds.payment.payment.domain.common.PaymentType;
import com.bds.payment.payment.domain.fundingPayment.FundingPayment;
import com.bds.payment.payment.domain.fundingPayment.FundingPaymentRepository;
import com.bds.payment.payment.domain.paymentHistory.PaymentHistory;
import com.bds.payment.payment.domain.paymentHistory.PaymentHistoryRepository;
import com.bds.payment.payment.domain.wallet.Wallet;
import com.bds.payment.payment.global.exception.BusinessException;
import com.bds.payment.payment.global.exception.ErrorCode;
import com.bds.payment.payment.presentation.request.SettlementBatchRequestDto.SettlementItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FundingSettlementProcessorUnitTest {

    @Mock private FundingPaymentRepository fundingPaymentRepository;
    @Mock private PaymentHistoryRepository paymentHistoryRepository;
    @Mock private WalletService walletService;

    @InjectMocks
    private FundingSettlementProcessor processor;

    @Nested
    @DisplayName("processSettlementItem()")
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

        @Test
        void 조회_실패시_FUNDING_NOT_FOUND_예외를_던진다() {
            // given
            SettlementItem item = new SettlementItem(999L, 1L, 10000L);
            given(fundingPaymentRepository.findByOrderId(item.orderId())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> processor.processSettlementItem(item))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FUNDING_NOT_FOUND);
                    });
        }

        @Test
        void 금액이_일치하지_않으면_SETTLEMENT_AMOUNT_MISMATCH_예외를_던진다() {
            // given
            SettlementItem item = new SettlementItem(1L, 1L, 99999L);
            FundingPayment fp = FundingPayment.builder()
                    .orderId(1L)
                    .amount(10000L)  // item.amount와 다름
                    .status(FundingPaymentStatus.SUCCESS)
                    .build();
            given(fundingPaymentRepository.findByOrderId(item.orderId())).willReturn(Optional.of(fp));

            // when & then
            assertThatThrownBy(() -> processor.processSettlementItem(item))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SETTLEMENT_AMOUNT_MISMATCH);
                    });
        }
    }

    @Nested
    @DisplayName("processRefundItem()")
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

        @Test
        void 존재하지_않는_거래를_환불하면_예외를_던진다() {
            // given
            given(fundingPaymentRepository.findByOrderId(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> processor.processRefundItem(999L, 1L, "USER_CANCEL"))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FUNDING_NOT_FOUND);
                    });
        }

        @Test
        void 타인의_거래를_환불하면_FUNDING_ACCESS_DENIED_예외를_던진다() {
            // given
            FundingPayment fp = FundingPayment.builder()
                    .orderId(1L)
                    .walletId(1L)  // 실제 소유자의 walletId
                    .amount(10000L)
                    .paymentType(PaymentType.INSTANT)
                    .status(FundingPaymentStatus.SUCCESS)
                    .build();

            given(fundingPaymentRepository.findByOrderId(1L)).willReturn(Optional.of(fp));
            given(walletService.getWalletId(2L)).willReturn(2L);  // 다른 사람의 walletId

            // when & then
            assertThatThrownBy(() -> processor.processRefundItem(1L, 2L, "USER_CANCEL"))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FUNDING_ACCESS_DENIED);
                    });
        }

        @Test
        void 이미_환불된_거래를_환불하면_FUNDING_ALREADY_REFUNDED_예외를_던진다() {
            // given
            FundingPayment fp = FundingPayment.builder()
                    .orderId(1L)
                    .walletId(1L)
                    .amount(10000L)
                    .paymentType(PaymentType.INSTANT)
                    .status(FundingPaymentStatus.REFUNDED)
                    .build();

            given(fundingPaymentRepository.findByOrderId(1L)).willReturn(Optional.of(fp));
            given(walletService.getWalletId(1L)).willReturn(1L);

            // when & then
            assertThatThrownBy(() -> processor.processRefundItem(1L, 1L, "USER_CANCEL"))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FUNDING_ALREADY_REFUNDED);
                    });
        }

        @Test
        void FAILED_상태의_거래를_환불하면_FUNDING_INVALID_STATUS_예외를_던진다() {
            // given
            FundingPayment fp = FundingPayment.builder()
                    .orderId(1L)
                    .walletId(1L)
                    .amount(10000L)
                    .paymentType(PaymentType.INSTANT)
                    .status(FundingPaymentStatus.FAILED)
                    .build();

            given(fundingPaymentRepository.findByOrderId(1L)).willReturn(Optional.of(fp));
            given(walletService.getWalletId(1L)).willReturn(1L);

            // when & then
            assertThatThrownBy(() -> processor.processRefundItem(1L, 1L, "USER_CANCEL"))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FUNDING_INVALID_STATUS);
                    });
        }
    }

    @Nested
    @DisplayName("creditCreatorForBatch()")
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

            given(fundingPaymentRepository.findUncreditedForUpdate(productId, FundingPaymentStatus.CONFIRMED)).willReturn(uncredited);
            given(walletService.charge(creatorId, 60000L)).willReturn(creatorWallet);

            // when
            processor.creditCreatorForBatch(creatorId, productId, "정산 확정");

            // then
            verify(walletService).charge(creatorId, 60000L);  // 10000 + 20000 + 30000
            verify(fundingPaymentRepository).updateCreditedAtBulk(
                    eq(List.of(1L, 2L, 3L)),
                    any(LocalDateTime.class)
            );
            verify(fundingPaymentRepository, never()).saveAll(any());
            verify(paymentHistoryRepository).save(any());
        }

        @Test
        void 미크레딧_항목이_없으면_아무_동작도_하지_않는다() {
            // given (재시도 시나리오)
            Long creatorId = 999L;
            Long productId = 100L;
            given(fundingPaymentRepository.findUncreditedForUpdate(productId, FundingPaymentStatus.CONFIRMED)).willReturn(List.of());

            // when
            processor.creditCreatorForBatch(creatorId, productId, "정산 확정");

            // then
            verify(walletService, never()).charge(any(), any());
            verify(fundingPaymentRepository, never()).updateCreditedAtBulk(any(), any());
            verify(fundingPaymentRepository, never()).saveAll(any());
            verify(paymentHistoryRepository, never()).save(any());
        }

        @Test
        void 미크레딧_500건이_있어도_walletCharge와_history는_각각_1번만_호출된다() {
            // given
            Long creatorId = 999L;
            Long productId = 100L;
            int itemCount = 500;
            long amountPerItem = 10000L;
            long expectedTotal = amountPerItem * itemCount;  // 5,000,000

            List<FundingPayment> uncredited = IntStream.range(0, itemCount)
                    .mapToObj(i -> FundingPayment.builder()
                            .id((long) (i + 1))
                            .amount(amountPerItem)
                            .status(FundingPaymentStatus.CONFIRMED)
                            .build())
                    .toList();

            Wallet creatorWallet = Wallet.builder()
                    .id(999L)
                    .memberId(creatorId)
                    .balance(expectedTotal)
                    .build();

            given(fundingPaymentRepository.findUncreditedForUpdate(productId, FundingPaymentStatus.CONFIRMED)).willReturn(uncredited);
            given(walletService.charge(creatorId, expectedTotal)).willReturn(creatorWallet);

            // when
            processor.creditCreatorForBatch(creatorId, productId, "정산 확정");

            // then
            // 1. 조회: 1번
            verify(fundingPaymentRepository, times(1)).findUncreditedForUpdate(productId, FundingPaymentStatus.CONFIRMED);

            // 2. 지갑 충전: 합산 금액으로 1번
            verify(walletService, times(1)).charge(creatorId, expectedTotal);

            // 3. UPDATE: 1번
            verify(fundingPaymentRepository, times(1)).updateCreditedAtBulk(anyList(), any(LocalDateTime.class));
            verify(fundingPaymentRepository, never()).saveAll(any());
            verify(fundingPaymentRepository, never()).save(any(FundingPayment.class));

            // 4. PaymentHistory: 1건만 생성
            verify(paymentHistoryRepository, times(1)).save(any(PaymentHistory.class));
        }

        @Test
        void 미크레딧_500건의_합산_금액이_정확히_계산된다() {
            // given - 다양한 금액이 섞여있는 경우
            Long creatorId = 999L;
            Long productId = 100L;

            List<FundingPayment> uncredited = IntStream.range(0, 500)
                    .mapToObj(i -> FundingPayment.builder()
                            .id((long) (i + 1))
                            .amount(1000L + i)  // 1000, 1001, 1002, ..., 1499
                            .status(FundingPaymentStatus.CONFIRMED)
                            .build())
                    .toList();

            long expectedTotal = uncredited.stream().mapToLong(FundingPayment::getAmount).sum();

            Wallet creatorWallet = Wallet.builder().id(999L).memberId(creatorId).balance(expectedTotal).build();

            given(fundingPaymentRepository.findUncreditedForUpdate(productId, FundingPaymentStatus.CONFIRMED)).willReturn(uncredited);
            given(walletService.charge(creatorId, expectedTotal)).willReturn(creatorWallet);

            // when
            processor.creditCreatorForBatch(creatorId, productId, "정산 확정");

            // then
            ArgumentCaptor<Long> amountCaptor = ArgumentCaptor.forClass(Long.class);
            verify(walletService).charge(eq(creatorId), amountCaptor.capture());
            assertThat(amountCaptor.getValue()).isEqualTo(expectedTotal);
            assertThat(amountCaptor.getValue()).isEqualTo(624750L);
        }

        @Test
        void updateCreditedAtBulk에_전달되는_ids에_모든_500건이_포함된다() {
            // given
            Long creatorId = 999L;
            Long productId = 100L;

            List<FundingPayment> uncredited = IntStream.range(0, 500)
                    .mapToObj(i -> FundingPayment.builder()
                            .id((long) (i + 1))
                            .amount(10000L)
                            .status(FundingPaymentStatus.CONFIRMED)
                            .build())
                    .toList();

            Wallet creatorWallet = Wallet.builder().id(999L).memberId(creatorId).balance(5_000_000L).build();

            given(fundingPaymentRepository.findUncreditedForUpdate(productId, FundingPaymentStatus.CONFIRMED)).willReturn(uncredited);
            given(walletService.charge(creatorId, 5_000_000L)).willReturn(creatorWallet);

            // when
            processor.creditCreatorForBatch(creatorId, productId, "정산 확정");

            // then
            ArgumentCaptor<List<Long>> idsCaptor = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(fundingPaymentRepository).updateCreditedAtBulk(idsCaptor.capture(), timeCaptor.capture());

            List<Long> capturedIds = idsCaptor.getValue();
            assertThat(capturedIds).hasSize(500);
            assertThat(capturedIds).containsExactlyElementsOf(IntStream.range(0, 500).mapToObj(i -> (long) (i + 1)).toList());
            assertThat(timeCaptor.getValue()).isNotNull();
        }
    }
}