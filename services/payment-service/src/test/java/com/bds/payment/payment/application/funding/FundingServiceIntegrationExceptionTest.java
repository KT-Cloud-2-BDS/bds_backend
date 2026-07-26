package com.bds.payment.payment.application.funding;

import com.bds.payment.payment.domain.common.FundingPaymentStatus;
import com.bds.payment.payment.domain.common.PaymentType;
import com.bds.payment.payment.global.exception.BusinessException;
import com.bds.payment.payment.global.exception.ErrorCode;
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
    @DisplayName("funding() 예외")
    class FundingExceptionTest {

        @Test
        void 이미_성공한_주문의_재요청은_예외없이_멱등_처리된다() {
            // given
            walletJpaRepository.save(WalletJpaEntity.builder().memberId(1L).balance(40000L).build());
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(1L, 1L, 100L, 10000L, PaymentType.INSTANT);
            fundingService.funding(dto);

            // when: 같은 주문 재요청 (예외 없이 정상 처리되어야 함)
            FundingPaymentResponseDto result = fundingService.funding(dto);

            // then
            assertThat(result).isNotNull();

            WalletJpaEntity wallet = walletJpaRepository.findByMemberId(1L).orElseThrow();
            assertThat(wallet.getBalance()).isEqualTo(30000L);
        }

        @Test
        void 잔액이_부족하면_예외없이_FAILED로_저장된다() {
            // given
            walletJpaRepository.save(WalletJpaEntity.builder().memberId(1L).balance(5000L).build());
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(1L, 1L, 100L, 10000L, PaymentType.INSTANT);

            // when: 예외 던지지 않음
            FundingPaymentResponseDto result = fundingService.funding(dto);

            // then
            assertThat(result).isNotNull();

            FundingPaymentJpaEntity fp = fundingPaymentJpaRepository.findByOrderId(dto.orderId()).orElseThrow();
            assertThat(fp.getStatus()).isEqualTo(FundingPaymentStatus.FAILED);
            assertThat(fp.getRetryCnt()).isEqualTo(1);

            WalletJpaEntity wallet = walletJpaRepository.findByMemberId(1L).orElseThrow();
            assertThat(wallet.getBalance()).isEqualTo(5000L);
        }

        @Test
        void 재시도_3회_초과된_주문에_대한_추가_요청도_예외없이_처리된다() {
            // given
            walletJpaRepository.save(WalletJpaEntity.builder().memberId(1L).balance(5000L).build());
            FundingPaymentRequestDto dto = new FundingPaymentRequestDto(1L, 1L, 100L, 10000L, PaymentType.INSTANT);

            fundingService.funding(dto);
            fundingService.funding(dto);
            fundingService.funding(dto);

            // when: 4회차 시도 (MAX_RETRY_EXCEEDED)
            FundingPaymentResponseDto result = fundingService.funding(dto);

            // then
            assertThat(result).isNotNull();

            FundingPaymentJpaEntity fp = fundingPaymentJpaRepository.findByOrderId(dto.orderId()).orElseThrow();
            assertThat(fp.getRetryCnt()).isEqualTo(3);  // 3에서 멈춤
        }
    }

    @Nested
    @DisplayName("refund() 예외")
    class RefundExceptionTest {

        @Test
        void 존재하지_않는_거래를_환불하면_예외를_던진다() {
            // given
            RefundRequestDto dto = new RefundRequestDto(
                    UuidCreator.getTimeOrderedEpoch(), 999L, 1L, 1L, 10000L, "USER_CANCEL"
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
                    UuidCreator.getTimeOrderedEpoch(), 1L, memberId, 1L, 10000L, "USER_CANCEL"
            ));

            // when & then
            RefundRequestDto secondRefund = new RefundRequestDto(
                    UuidCreator.getTimeOrderedEpoch(), 1L, memberId, 1L, 10000L, "USER_CANCEL"
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
                    UuidCreator.getTimeOrderedEpoch(), 1L, otherId, 1L, 10000L, "USER_CANCEL"
            );
            assertThatThrownBy(() -> fundingService.refund(invalidRefund))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FUNDING_ACCESS_DENIED);
                    });
        }
    }

    @Nested
    @DisplayName("confirmSettlement() 예외")
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
    @DisplayName("confirmReservedFunding() 예외")
    class ConfirmReservedFundingExceptionTest {

        @Test
        void 잔액이_부족한_항목은_FAILED로_저장되고_나머지는_정상_처리된다() {
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

            // 성공한 건은 CONFIRMED
            FundingPaymentJpaEntity successFp = fundingPaymentJpaRepository.findByOrderId(201L).orElseThrow();
            assertThat(successFp.getStatus()).isEqualTo(FundingPaymentStatus.CONFIRMED);

            FundingPaymentJpaEntity failedFp = fundingPaymentJpaRepository.findByOrderId(202L).orElseThrow();
            assertThat(failedFp.getStatus()).isEqualTo(FundingPaymentStatus.FAILED);
            assertThat(failedFp.getRetryCnt()).isEqualTo(1);
        }

        @Test
        void 재시도_3회_초과된_예약펀딩은_MAX_RETRY_EXCEEDED로_처리된다() {
            // given: 잔액 부족한 상태에서 3회 실패
            Long creatorId = 999L;
            Long productId = 100L;
            walletJpaRepository.save(WalletJpaEntity.builder().memberId(creatorId).balance(0L).build());
            walletJpaRepository.save(WalletJpaEntity.builder().memberId(1L).balance(500L).build());

            SettlementBatchRequestDto batchDto = new SettlementBatchRequestDto(
                    UuidCreator.getTimeOrderedEpoch(),
                    null,
                    creatorId,
                    productId,
                    List.of(new SettlementItem(201L, 1L, 10000L))
            );

            fundingService.confirmReservedFunding(batchDto);
            fundingService.confirmReservedFunding(batchDto);
            fundingService.confirmReservedFunding(batchDto);

            // when: 4회차 시도
            SettlementResultResponseDto result = fundingService.confirmReservedFunding(batchDto);

            // then
            assertThat(result.successItems()).isEmpty();
            assertThat(result.failedItems()).hasSize(1);

            FundingPaymentJpaEntity fp = fundingPaymentJpaRepository.findByOrderId(201L).orElseThrow();
            assertThat(fp.getStatus()).isEqualTo(FundingPaymentStatus.FAILED);
            assertThat(fp.getRetryCnt()).isEqualTo(3);
        }

        @Test
        void 창작자_지갑이_없으면_전체_배치가_실패한다() {
            // given: 창작자 지갑은 없음
            Long creatorId = 999L;
            Long productId = 100L;
            walletJpaRepository.save(WalletJpaEntity.builder().memberId(1L).balance(30000L).build());

            SettlementBatchRequestDto batchDto = new SettlementBatchRequestDto(
                    UuidCreator.getTimeOrderedEpoch(),
                    null,
                    creatorId,
                    productId,
                    List.of(new SettlementItem(201L, 1L, 10000L))
            );

            // when & then
            assertThatThrownBy(() -> fundingService.confirmReservedFunding(batchDto)).isInstanceOf(BusinessException.class);
        }
    }
}