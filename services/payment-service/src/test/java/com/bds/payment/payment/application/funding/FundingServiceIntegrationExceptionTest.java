package com.bds.payment.payment.application.funding;

import com.bds.payment.payment.domain.common.PaymentType;
import com.bds.payment.payment.global.exception.BusinessException;
import com.bds.payment.payment.global.exception.ErrorCode;
import com.bds.payment.payment.infrastructure.persistence.fundingPayment.FundingHistoryJpaRepository;
import com.bds.payment.payment.infrastructure.persistence.paymentHistory.PaymentHistoryJpaRepository;
import com.bds.payment.payment.infrastructure.persistence.wallet.WalletJpaEntity;
import com.bds.payment.payment.infrastructure.persistence.wallet.WalletJpaRepository;
import com.bds.payment.payment.presentation.request.FundingPaymentRequestDto;
import com.bds.payment.payment.presentation.request.RefundRequestDto;
import com.bds.payment.payment.presentation.request.SettlementBatchRequestDto;
import com.bds.payment.payment.presentation.response.SettlementResultResponseDto;
import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static com.bds.payment.payment.presentation.request.SettlementBatchRequestDto.SettlementItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class FundingServiceIntegrationExceptionTest {

    @Autowired private FundingService fundingService;

    @Autowired private FundingHistoryJpaRepository fundingPaymentJpaRepository;
    @Autowired private WalletJpaRepository walletJpaRepository;
    @Autowired private PaymentHistoryJpaRepository paymentHistoryJpaRepository;

    @AfterEach
    void cleanUp() {
        paymentHistoryJpaRepository.deleteAll();
        fundingPaymentJpaRepository.deleteAll();
        walletJpaRepository.deleteAll();
    }

    @Nested
    @DisplayName("펀딩 결제 예외")
    class FundingExceptionTest {

        @Test
        void 중복된_주문이면_예외를_던진다() {
            // given
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(1L, 1L, 100L, 10000L, PaymentType.INSTANT);
            WalletJpaEntity wallet = walletJpaRepository.save(
                    WalletJpaEntity.builder().memberId(dto.memberId()).balance(40000L).build()
            );
            fundingService.funding(dto);

            // when & then
            FundingPaymentRequestDto duplicated = new FundingPaymentRequestDto(1L, 1L, 100L, 10000L, PaymentType.INSTANT);
            assertThatThrownBy(() -> fundingService.funding(duplicated))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FUNDING_DUPLICATED);
                    });
        }

        @Test
        void INSTANT_잔액이_부족하면_예외를_던진다() {
            // given
            walletJpaRepository.save(WalletJpaEntity.builder().memberId(1L).balance(5000L).build());
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(1L, 1L, 100L, 10000L, PaymentType.INSTANT);

            // when & then
            assertThatThrownBy(() -> fundingService.funding(dto))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_INSUFFICIENT_BALANCE);
                    });
        }
    }

    @Nested
    @DisplayName("환불 예외")
    class RefundExceptionTest {

        @Test
        void 존재하지_않는_거래를_환불하면_예외를_던진다() {
            // given
            RefundRequestDto dto = new RefundRequestDto(
                    UuidCreator.getTimeOrderedEpoch(), 999L, 1L, 10000L, "USER_CANCEL"
            );

            // when & then
            assertThatThrownBy(() -> fundingService.refund(dto))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FUNDING_NOT_FOUND);
                    });
        }

        @Test
        void 이미_환불된_거래를_환불하면_예외를_던진다() {
            // given
            Long memberId = 1L;
            walletJpaRepository.save(WalletJpaEntity.builder().memberId(memberId).balance(30000L).build());
            fundingService.funding(new FundingPaymentRequestDto(1L, memberId, 100L, 10000L, PaymentType.INSTANT));
            fundingService.refund(new RefundRequestDto(
                    UuidCreator.getTimeOrderedEpoch(), 1L, memberId, 10000L, "USER_CANCEL"
            ));

            // when & then
            RefundRequestDto secondRefund = new RefundRequestDto(
                    UuidCreator.getTimeOrderedEpoch(), 1L, memberId, 10000L, "USER_CANCEL"
            );
            assertThatThrownBy(() -> fundingService.refund(secondRefund))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FUNDING_ALREADY_REFUNDED);
                    });
        }

        @Test
        void 타인의_거래를_환불하면_예외를_던진다() {
            // given
            Long ownerId = 1L;
            Long otherId = 2L;
            walletJpaRepository.save(WalletJpaEntity.builder().memberId(ownerId).balance(30000L).build());
            walletJpaRepository.save(WalletJpaEntity.builder().memberId(otherId).balance(30000L).build());
            fundingService.funding(new FundingPaymentRequestDto(1L, ownerId, 100L, 10000L, PaymentType.INSTANT));

            // when & then
            RefundRequestDto invalidRefund = new RefundRequestDto(
                    UuidCreator.getTimeOrderedEpoch(), 1L, otherId, 10000L, "USER_CANCEL"
            );
            assertThatThrownBy(() -> fundingService.refund(invalidRefund))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FUNDING_ACCESS_DENIED);
                    });
        }
    }

    @Nested
    @DisplayName("정산확정 배치 예외")
    class ConfirmSettlementExceptionTest {

        @Test
        void 금액이_불일치하면_해당_항목만_실패로_처리된다() {
            // given
            Long creatorId = 999L;
            Long productId = 100L;
            walletJpaRepository.save(WalletJpaEntity.builder().memberId(creatorId).balance(0L).build());

            for (int i = 0; i < 2; i++) {
                Long memberId = (long) (i + 1);
                Long orderId = (long) (101 + i);
                walletJpaRepository.save(WalletJpaEntity.builder().memberId(memberId).balance(30000L).build());
                fundingService.funding(new FundingPaymentRequestDto(
                        orderId, memberId, productId, 10000L, PaymentType.INSTANT
                ));
            }

            SettlementBatchRequestDto batchDto = new SettlementBatchRequestDto(
                    UuidCreator.getTimeOrderedEpoch(),
                    null,
                    creatorId,
                    productId,
                    List.of(
                            new SettlementItem(101L, 1L, 10000L),
                            new SettlementItem(102L, 2L, 99999L)  // 금액 불일치
                    )
            );

            // when
            SettlementResultResponseDto result = fundingService.confirmSettlement(batchDto);

            // then
            assertThat(result.successItems()).hasSize(1);
            assertThat(result.failedItems()).hasSize(1);
        }

        @Test
        void 창작자_지갑이_없으면_전체_배치가_실패한다() {
            // given: 창작자 지갑은 없음
            Long creatorId = 999L;
            Long productId = 100L;

            walletJpaRepository.save(WalletJpaEntity.builder().memberId(1L).balance(30000L).build());
            fundingService.funding(new FundingPaymentRequestDto(101L, 1L, productId, 10000L, PaymentType.INSTANT));

            SettlementBatchRequestDto batchDto = new SettlementBatchRequestDto(
                    UuidCreator.getTimeOrderedEpoch(),
                    null,
                    creatorId,
                    productId,
                    List.of(new SettlementItem(101L, 1L, 10000L))
            );

            // when & then
            assertThatThrownBy(() -> fundingService.confirmSettlement(batchDto))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("예약펀딩확정 배치 예외")
    class ConfirmReservedFundingExceptionTest {

        @Test
        void 잔액이_부족한_항목은_실패로_처리되고_나머지는_진행된다() {
            // given
            Long creatorId = 999L;
            Long productId = 100L;
            walletJpaRepository.save(WalletJpaEntity.builder().memberId(creatorId).balance(0L).build());

            walletJpaRepository.save(WalletJpaEntity.builder().memberId(1L).balance(30000L).build());
            walletJpaRepository.save(WalletJpaEntity.builder().memberId(2L).balance(500L).build());  // 잔액 부족

            SettlementBatchRequestDto batchDto = new SettlementBatchRequestDto(
                    UuidCreator.getTimeOrderedEpoch(),
                    null,
                    creatorId,
                    productId,
                    List.of(
                            new SettlementItem(201L, 1L, 10000L),
                            new SettlementItem(202L, 2L, 10000L)
                    )
            );

            // when
            SettlementResultResponseDto result = fundingService.confirmReservedFunding(batchDto);

            // then
            assertThat(result.successItems()).hasSize(1);
            assertThat(result.failedItems()).hasSize(1);

            // 잔액 부족한 유저의 주문은 저장 안 됨
            assertThat(fundingPaymentJpaRepository.findByOrderId(202L)).isEmpty();

            // 성공한 유저의 주문은 저장됨
            assertThat(fundingPaymentJpaRepository.findByOrderId(201L)).isPresent();
        }
    }
}