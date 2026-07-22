package com.bds.payment.payment.application.funding;

import com.bds.payment.payment.application.wallet.WalletService;
import com.bds.payment.payment.domain.common.FundingPaymentStatus;
import com.bds.payment.payment.domain.common.PaymentType;
import com.bds.payment.payment.infrastructure.persistence.fundingPayment.FundingHistoryJpaRepository;
import com.bds.payment.payment.infrastructure.persistence.fundingPayment.FundingPaymentJpaEntity;
import com.bds.payment.payment.infrastructure.persistence.paymentHistory.PaymentHistoryJpaRepository;
import com.bds.payment.payment.infrastructure.persistence.wallet.WalletJpaEntity;
import com.bds.payment.payment.infrastructure.persistence.wallet.WalletJpaRepository;
import com.bds.payment.payment.presentation.request.FundingPaymentRequestDto;
import com.bds.payment.payment.presentation.request.RefundRequestDto;
import com.bds.payment.payment.presentation.request.SettlementBatchRequestDto;
import com.bds.payment.payment.presentation.response.FundingPaymentResponseDto;
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

@SpringBootTest
@ActiveProfiles("test")
class FundingServiceIntegrationTest {

    @Autowired private FundingService fundingService;
    @Autowired private WalletService walletService;

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
    @DisplayName("펀딩 결제")
    class FundingTest {

        @Test
        void INSTANT_결제를_정상_처리한다() {
            // given
            walletJpaRepository.save(WalletJpaEntity.builder().memberId(1L).balance(40000L).build());
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(1L, 1L, 100L, 10000L, PaymentType.INSTANT);

            // when
            FundingPaymentResponseDto result = fundingService.funding(dto);

            // then
            assertThat(result).isNotNull();
            assertThat(fundingPaymentJpaRepository.findByOrderId(dto.orderId())).isPresent();

            Long walletId = walletService.getWalletId(dto.memberId());
            WalletJpaEntity wallet = walletJpaRepository.findById(walletId).orElseThrow();
            assertThat(wallet.getBalance()).isEqualTo(30000L);
        }

        @Test
        void RESERVED_결제는_지갑_차감_없이_처리한다() {
            // given
            walletJpaRepository.save(WalletJpaEntity.builder().memberId(1L).balance(0L).build());
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(2L, 1L, 100L, 10000L, PaymentType.RESERVED);

            // when
            FundingPaymentResponseDto result = fundingService.funding(dto);

            // then
            assertThat(result).isNotNull();
            assertThat(fundingPaymentJpaRepository.findByOrderId(dto.orderId())).isPresent();

            Long walletId = walletService.getWalletId(dto.memberId());
            WalletJpaEntity wallet = walletJpaRepository.findById(walletId).orElseThrow();
            assertThat(wallet.getBalance()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("환불")
    class RefundTest {

        @Test
        void INSTANT_환불시_지갑에_금액이_충전된다() {
            // given
            walletJpaRepository.save(WalletJpaEntity.builder().memberId(1L).balance(30000L).build());
            fundingService.funding(new FundingPaymentRequestDto(1L, 1L, 100L, 10000L, PaymentType.INSTANT));

            // when
            fundingService.refund(new RefundRequestDto(
                    UuidCreator.getTimeOrderedEpoch(), 1L, 1L, 1L, 10000L, "USER_CANCEL"
            ));

            // then
            Long walletId = walletService.getWalletId(1L);
            WalletJpaEntity wallet = walletJpaRepository.findById(walletId).orElseThrow();
            assertThat(wallet.getBalance()).isEqualTo(30000L);

            FundingPaymentJpaEntity fp = fundingPaymentJpaRepository.findByOrderId(1L).orElseThrow();
            assertThat(fp.getStatus()).isEqualTo(FundingPaymentStatus.REFUNDED);
        }
    }

    @Nested
    @DisplayName("정산확정 배치")
    class ConfirmSettlementTest {

        @Test
        void 정산확정시_상태가_CONFIRMED로_변경되고_창작자에게_크레딧된다() {
            // given
            Long creatorId = 999L;
            Long productId = 100L;
            walletJpaRepository.save(WalletJpaEntity.builder().memberId(creatorId).balance(0L).build());

            List<Long> memberIds = List.of(1L, 2L, 3L);
            List<Long> orderIds = List.of(101L, 102L, 103L);
            for (int i = 0; i < 3; i++) {
                walletJpaRepository.save(WalletJpaEntity.builder().memberId(memberIds.get(i)).balance(30000L).build());
                fundingService.funding(new FundingPaymentRequestDto(
                        orderIds.get(i), memberIds.get(i), productId, 10000L, PaymentType.INSTANT
                ));
            }

            SettlementBatchRequestDto batchDto = new SettlementBatchRequestDto(
                    UuidCreator.getTimeOrderedEpoch(),
                    null,
                    creatorId,
                    productId,
                    List.of(
                            new SettlementItem(101L, 1L, 10000L),
                            new SettlementItem(102L, 2L, 10000L),
                            new SettlementItem(103L, 3L, 10000L)
                    )
            );

            // when
            SettlementResultResponseDto result = fundingService.confirmSettlement(batchDto);

            // then
            assertThat(result.successItems()).hasSize(3);
            assertThat(result.failedItems()).isEmpty();

            for (Long orderId : orderIds) {
                FundingPaymentJpaEntity fp = fundingPaymentJpaRepository.findByOrderId(orderId).orElseThrow();
                assertThat(fp.getStatus()).isEqualTo(FundingPaymentStatus.CONFIRMED);
                assertThat(fp.getCreditedAt()).isNotNull();
            }

            Long creatorWalletId = walletService.getWalletId(creatorId);
            WalletJpaEntity creatorWallet = walletJpaRepository.findById(creatorWalletId).orElseThrow();
            assertThat(creatorWallet.getBalance()).isEqualTo(30000L);
        }

        @Test
        void 재시도시_이미_크레딧된_항목은_스킵되어_이중_정산되지_않는다() {
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
                            new SettlementItem(102L, 2L, 10000L)
                    )
            );
            fundingService.confirmSettlement(batchDto);

            Long creatorWalletId = walletService.getWalletId(creatorId);
            long balanceAfterFirst = walletJpaRepository.findById(creatorWalletId).orElseThrow().getBalance();
            assertThat(balanceAfterFirst).isEqualTo(20000L);

            // when
            SettlementResultResponseDto retryResult = fundingService.confirmSettlement(batchDto);

            // then
            assertThat(retryResult.successItems())
                    .allMatch(item -> "ALREADY_CONFIRMED".equals(item.message()));

            long balanceAfterRetry = walletJpaRepository.findById(creatorWalletId).orElseThrow().getBalance();
            assertThat(balanceAfterRetry).isEqualTo(20000L);
        }
    }

    @Nested
    @DisplayName("예약펀딩확정 배치")
    class ConfirmReservedFundingTest {

        @Test
        void 예약펀딩_확정시_funding_payment가_신규_생성되고_지갑에서_차감된다() {
            // given
            Long creatorId = 999L;
            Long productId = 100L;
            walletJpaRepository.save(WalletJpaEntity.builder().memberId(creatorId).balance(0L).build());

            for (int i = 0; i < 2; i++) {
                Long memberId = (long) (i + 1);
                walletJpaRepository.save(WalletJpaEntity.builder().memberId(memberId).balance(30000L).build());
            }

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
            assertThat(result.successItems()).hasSize(2);
            assertThat(result.failedItems()).isEmpty();

            for (Long orderId : List.of(201L, 202L)) {
                FundingPaymentJpaEntity fp = fundingPaymentJpaRepository.findByOrderId(orderId).orElseThrow();
                assertThat(fp.getStatus()).isEqualTo(FundingPaymentStatus.CONFIRMED);
                assertThat(fp.getPaymentType()).isEqualTo(PaymentType.RESERVED);
                assertThat(fp.getCreditedAt()).isNotNull();
            }

            for (int i = 0; i < 2; i++) {
                Long walletId = walletService.getWalletId((long) (i + 1));
                WalletJpaEntity wallet = walletJpaRepository.findById(walletId).orElseThrow();
                assertThat(wallet.getBalance()).isEqualTo(20000L);
            }

            Long creatorWalletId = walletService.getWalletId(creatorId);
            WalletJpaEntity creatorWallet = walletJpaRepository.findById(creatorWalletId).orElseThrow();
            assertThat(creatorWallet.getBalance()).isEqualTo(20000L);
        }

        @Test
        void 재시도시_이미_생성된_항목은_ALREADY_CONFIRMED로_스킵된다() {
            // given
            Long creatorId = 999L;
            Long productId = 100L;
            walletJpaRepository.save(WalletJpaEntity.builder().memberId(creatorId).balance(0L).build());

            for (int i = 0; i < 2; i++) {
                Long memberId = (long) (i + 1);
                walletJpaRepository.save(WalletJpaEntity.builder().memberId(memberId).balance(30000L).build());
            }

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

            // 첫 배치 실행
            fundingService.confirmReservedFunding(batchDto);

            // when: 같은 배치 재시도
            SettlementResultResponseDto retryResult = fundingService.confirmReservedFunding(batchDto);

            // then
            assertThat(retryResult.successItems()).hasSize(2);
            assertThat(retryResult.successItems())
                    .allMatch(item -> "ALREADY_CONFIRMED".equals(item.message()));
            assertThat(retryResult.failedItems()).isEmpty();
        }
    }

    @Nested
    @DisplayName("펀딩실패 환불 배치")
    class RefundFailedFundingTest {

        @Test
        void 펀딩실패시_모든_항목이_환불된다() {
            // given
            for (int i = 0; i < 2; i++) {
                Long memberId = (long) (i + 1);
                Long orderId = (long) (301 + i);
                walletJpaRepository.save(WalletJpaEntity.builder().memberId(memberId).balance(30000L).build());
                fundingService.funding(new FundingPaymentRequestDto(
                        orderId, memberId, 100L, 10000L, PaymentType.INSTANT
                ));
            }

            SettlementBatchRequestDto batchDto = new SettlementBatchRequestDto(
                    UuidCreator.getTimeOrderedEpoch(),
                    null,
                    999L,
                    100L,
                    List.of(
                            new SettlementItem(301L, 1L, 10000L),
                            new SettlementItem(302L, 2L, 10000L)
                    )
            );

            // when
            SettlementResultResponseDto result = fundingService.refundFailedFunding(batchDto);

            // then
            assertThat(result.successItems()).hasSize(2);
            assertThat(result.failedItems()).isEmpty();

            for (int i = 0; i < 2; i++) {
                Long orderId = (long) (301 + i);
                Long memberId = (long) (i + 1);

                FundingPaymentJpaEntity fp = fundingPaymentJpaRepository.findByOrderId(orderId).orElseThrow();
                assertThat(fp.getStatus()).isEqualTo(FundingPaymentStatus.REFUNDED);

                Long walletId = walletService.getWalletId(memberId);
                WalletJpaEntity wallet = walletJpaRepository.findById(walletId).orElseThrow();
                assertThat(wallet.getBalance()).isEqualTo(30000L);
            }
        }

        @Test
        void 이미_환불된_항목은_ALREADY_REFUNDED로_표시된다() {
            // given
            Long memberId = 1L;
            Long orderId = 401L;
            walletJpaRepository.save(WalletJpaEntity.builder().memberId(memberId).balance(30000L).build());
            fundingService.funding(new FundingPaymentRequestDto(
                    orderId, memberId, 100L, 10000L, PaymentType.INSTANT
            ));

            // 먼저 환불
            fundingService.refund(new RefundRequestDto(
                    UuidCreator.getTimeOrderedEpoch(), orderId, memberId, 1L, 10000L, "USER_CANCEL"
            ));

            // when: 배치로 다시 환불 시도
            SettlementBatchRequestDto batchDto = new SettlementBatchRequestDto(
                    UuidCreator.getTimeOrderedEpoch(),
                    null,
                    999L,
                    100L,
                    List.of(new SettlementItem(orderId, memberId, 10000L))
            );
            SettlementResultResponseDto result = fundingService.refundFailedFunding(batchDto);

            // then
            assertThat(result.successItems()).hasSize(1);
            assertThat(result.successItems().getFirst().message()).isEqualTo("ALREADY_REFUNDED");
            assertThat(result.failedItems()).isEmpty();
        }
    }
}