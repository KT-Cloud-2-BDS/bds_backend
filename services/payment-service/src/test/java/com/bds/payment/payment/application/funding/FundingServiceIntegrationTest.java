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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

    @PersistenceContext
    private EntityManager entityManager;

    @AfterEach
    void cleanUp() {
        paymentHistoryJpaRepository.deleteAll();
        fundingPaymentJpaRepository.deleteAll();
        walletJpaRepository.deleteAll();
    }

    @Nested
    @DisplayName("funding()")
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

            FundingPaymentJpaEntity fp = fundingPaymentJpaRepository.findByOrderId(dto.orderId()).orElseThrow();
            assertThat(fp.getStatus()).isEqualTo(FundingPaymentStatus.SUCCESS);
            assertThat(fp.getRetryCnt()).isEqualTo(0);

            Long walletId = walletService.getWalletId(dto.memberId());
            WalletJpaEntity wallet = walletJpaRepository.findById(walletId).orElseThrow();
            assertThat(wallet.getBalance()).isEqualTo(30000L);
        }

        @Test
        void 잔액_부족시_FAILED로_저장되고_retryCnt가_증가한다() {
            // given
            walletJpaRepository.save(WalletJpaEntity.builder().memberId(1L).balance(5000L).build());
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(1L, 1L, 100L, 10000L, PaymentType.INSTANT);

            // when
            fundingService.funding(dto);

            // then
            FundingPaymentJpaEntity fp = fundingPaymentJpaRepository.findByOrderId(dto.orderId()).orElseThrow();
            assertThat(fp.getStatus()).isEqualTo(FundingPaymentStatus.FAILED);
            assertThat(fp.getRetryCnt()).isEqualTo(1);

            Long walletId = walletService.getWalletId(dto.memberId());
            WalletJpaEntity wallet = walletJpaRepository.findById(walletId).orElseThrow();
            assertThat(wallet.getBalance()).isEqualTo(5000L);
        }

        @Test
        void 재시도_결제_성공시_기존_record가_SUCCESS로_전이된다() {
            // given: 1차 실패
            walletJpaRepository.save(WalletJpaEntity.builder().memberId(1L).balance(5000L).build());
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(1L, 1L, 100L, 10000L, PaymentType.INSTANT);
            fundingService.funding(dto);

            walletService.charge(1L, 20000L);

            // when: 2차 재시도
            fundingService.funding(dto);

            // then
            FundingPaymentJpaEntity fp = fundingPaymentJpaRepository.findByOrderId(dto.orderId()).orElseThrow();
            assertThat(fp.getStatus()).isEqualTo(FundingPaymentStatus.SUCCESS);
            assertThat(fp.getRetryCnt()).isEqualTo(1);  // 1차 실패 카운트 유지

            Long walletId = walletService.getWalletId(dto.memberId());
            WalletJpaEntity wallet = walletJpaRepository.findById(walletId).orElseThrow();
            assertThat(wallet.getBalance()).isEqualTo(15000L);
        }

        @Test
        void 재시도_3회_초과시_MAX_RETRY_EXCEEDED로_처리된다() {
            // given
            walletJpaRepository.save(WalletJpaEntity.builder().memberId(1L).balance(5000L).build());
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(1L, 1L, 100L, 10000L, PaymentType.INSTANT);

            fundingService.funding(dto);  // retryCnt=1
            fundingService.funding(dto);  // retryCnt=2
            fundingService.funding(dto);  // retryCnt=3

            // when: 4회차 시도
            fundingService.funding(dto);

            // then
            FundingPaymentJpaEntity fp = fundingPaymentJpaRepository.findByOrderId(dto.orderId()).orElseThrow();
            assertThat(fp.getStatus()).isEqualTo(FundingPaymentStatus.FAILED);
            assertThat(fp.getRetryCnt()).isEqualTo(3);  // 3에서 멈춤

            Long walletId = walletService.getWalletId(dto.memberId());
            WalletJpaEntity wallet = walletJpaRepository.findById(walletId).orElseThrow();
            assertThat(wallet.getBalance()).isEqualTo(5000L);
        }

        @Test
        void 이미_성공한_주문에_대한_재요청은_멱등_처리된다() {
            // given
            walletJpaRepository.save(WalletJpaEntity.builder().memberId(1L).balance(40000L).build());
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(1L, 1L, 100L, 10000L, PaymentType.INSTANT);
            fundingService.funding(dto);

            // when: 같은 주문 재요청
            fundingService.funding(dto);

            // then
            FundingPaymentJpaEntity fp = fundingPaymentJpaRepository.findByOrderId(dto.orderId()).orElseThrow();
            assertThat(fp.getStatus()).isEqualTo(FundingPaymentStatus.SUCCESS);

            Long walletId = walletService.getWalletId(dto.memberId());
            WalletJpaEntity wallet = walletJpaRepository.findById(walletId).orElseThrow();
            assertThat(wallet.getBalance()).isEqualTo(30000L);
        }
    }

    @Nested
    @DisplayName("refund()")
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
    @DisplayName("confirmSettlement()")
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
            entityManager.clear();
            long balanceAfterFirst = walletJpaRepository.findById(creatorWalletId).orElseThrow().getBalance();
            assertThat(balanceAfterFirst).isEqualTo(20000L);

            // when
            SettlementResultResponseDto retryResult = fundingService.confirmSettlement(batchDto);

            // then
            assertThat(retryResult.successItems()).allMatch(item -> "ALREADY_CONFIRMED".equals(item.message()));

            long balanceAfterRetry = walletJpaRepository.findById(creatorWalletId).orElseThrow().getBalance();
            assertThat(balanceAfterRetry).isEqualTo(20000L);
        }
    }

    @Nested
    @DisplayName("confirmReservedFunding()")
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
        void 잔액_부족한_항목은_FAILED로_저장되고_나머지는_정상_처리된다() {
            // given
            Long creatorId = 999L;
            Long productId = 100L;
            walletJpaRepository.save(WalletJpaEntity.builder().memberId(creatorId).balance(0L).build());

            walletJpaRepository.save(WalletJpaEntity.builder().memberId(1L).balance(30000L).build());
            walletJpaRepository.save(WalletJpaEntity.builder().memberId(2L).balance(5000L).build());  // 잔액 부족
            walletJpaRepository.save(WalletJpaEntity.builder().memberId(3L).balance(30000L).build());

            SettlementBatchRequestDto batchDto = new SettlementBatchRequestDto(
                    UuidCreator.getTimeOrderedEpoch(),
                    null,
                    creatorId,
                    productId,
                    List.of(
                            new SettlementItem(201L, 1L, 10000L),
                            new SettlementItem(202L, 2L, 10000L),
                            new SettlementItem(203L, 3L, 10000L)
                    )
            );

            // when
            SettlementResultResponseDto result = fundingService.confirmReservedFunding(batchDto);

            // then
            assertThat(result.successItems()).hasSize(2);
            assertThat(result.failedItems()).hasSize(1);

            for (Long orderId : List.of(201L, 203L)) {
                FundingPaymentJpaEntity fp = fundingPaymentJpaRepository.findByOrderId(orderId).orElseThrow();
                assertThat(fp.getStatus()).isEqualTo(FundingPaymentStatus.CONFIRMED);
            }

            FundingPaymentJpaEntity failedFp = fundingPaymentJpaRepository.findByOrderId(202L).orElseThrow();
            assertThat(failedFp.getStatus()).isEqualTo(FundingPaymentStatus.FAILED);
            assertThat(failedFp.getRetryCnt()).isEqualTo(1);

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

            fundingService.confirmReservedFunding(batchDto);

            // when: 같은 배치 재시도
            SettlementResultResponseDto retryResult = fundingService.confirmReservedFunding(batchDto);

            // then
            assertThat(retryResult.successItems()).hasSize(2);
            assertThat(retryResult.successItems()).allMatch(item -> "ALREADY_CONFIRMED".equals(item.message()));
            assertThat(retryResult.failedItems()).isEmpty();
        }

        @Test
        void 실패한_예약펀딩_재시도시_기존_record가_재사용되어_retryCnt가_증가한다() {
            // given: 잔액 부족으로 실패
            Long creatorId = 999L;
            Long productId = 100L;
            walletJpaRepository.save(WalletJpaEntity.builder().memberId(creatorId).balance(0L).build());
            walletJpaRepository.save(WalletJpaEntity.builder().memberId(1L).balance(5000L).build());

            SettlementBatchRequestDto batchDto = new SettlementBatchRequestDto(
                    UuidCreator.getTimeOrderedEpoch(),
                    null,
                    creatorId,
                    productId,
                    List.of(new SettlementItem(201L, 1L, 10000L))
            );
            fundingService.confirmReservedFunding(batchDto);

            FundingPaymentJpaEntity fp1 = fundingPaymentJpaRepository.findByOrderId(201L).orElseThrow();
            assertThat(fp1.getStatus()).isEqualTo(FundingPaymentStatus.FAILED);
            assertThat(fp1.getRetryCnt()).isEqualTo(1);

            // when: 잔액 여전히 부족한 상태로 재시도
            fundingService.confirmReservedFunding(batchDto);

            // then
            FundingPaymentJpaEntity fp2 = fundingPaymentJpaRepository.findByOrderId(201L).orElseThrow();
            assertThat(fp2.getStatus()).isEqualTo(FundingPaymentStatus.FAILED);
            assertThat(fp2.getRetryCnt()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("refundFailedFunding()")
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