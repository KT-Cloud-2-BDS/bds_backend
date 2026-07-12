package com.bds.payment.payment.application.wallet;

import com.bds.payment.payment.global.exception.BusinessException;
import com.bds.payment.payment.global.exception.ErrorCode;
import com.bds.payment.payment.infrastructure.persistence.wallet.WalletJpaEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WalletServiceIntegrationExceptionTest {

    @Autowired private WalletService walletService;

    @Autowired private EntityManager em;

    @Nested
    @DisplayName("지갑 조회 통합 예외 테스트")
    class GetWalletExceptionTest {

        @Test
        void 지갑이_없으면_getWalletResponseDto는_예외를_던진다() {
            Long memberId = Long.MAX_VALUE;

            assertThatThrownBy(() -> walletService.getWalletResponseDto(memberId))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_NOT_FOUND);
                        assertThat(ex.getMessage()).isEqualTo(ErrorCode.WALLET_NOT_FOUND.getMessage());
                    });
        }

        @Test
        void 지갑이_없으면_getWallet은_예외를_던진다() {
            // given
            Long memberId = 1L;
            // when & then
            assertThatThrownBy(() -> walletService.getWallet(memberId))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_NOT_FOUND);
                        assertThat(ex.getMessage()).isEqualTo(ErrorCode.WALLET_NOT_FOUND.getMessage());
                    });
        }

        @Test
        void 지갑이_없으면_getWalletId는_예외를_던진다() {
            // given
            Long memberId = 1L;

            // when & then
            assertThatThrownBy(() -> walletService.getWalletId(memberId))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_NOT_FOUND);
                        assertThat(ex.getMessage()).isEqualTo(ErrorCode.WALLET_NOT_FOUND.getMessage());
                    });
        }

    }

    @Nested
    @DisplayName("지갑 생성 통합 예외 테스트")
    class CreateWalletExceptionTest {

        @Test
        void 이미_지갑이_있으면_예외를_던지고_저장하지_않는다() {
            // given
            Long memberId = 1L;
            WalletJpaEntity walletJpaEntity = WalletJpaEntity.builder()
                    .memberId(memberId)
                    .balance(0L)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            em.persist(walletJpaEntity);
            em.flush();
            em.clear();
            // when & then
            assertThatThrownBy(() -> walletService.createWallet(memberId))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_ALREADY_EXISTS);
                        assertThat(ex.getMessage()).isEqualTo(ErrorCode.WALLET_ALREADY_EXISTS.getMessage());
                    });

            em.clear();

            int walletCount = em.createQuery("select w from WalletJpaEntity w", WalletJpaEntity.class)
                    .getResultList().size();

            assertThat(walletCount).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("지갑 충전 통합 예외 테스트")
    class ChargeExceptionTest {

        @Test
        void 지갑이_없으면_충전_시_예외를_던지고_저장하지_않는다() {
            // given
            Long memberId = Long.MAX_VALUE;
            Long amount = 10000L;

            // when & then
            assertThatThrownBy(() -> walletService.charge(memberId, amount))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_NOT_FOUND);
                        assertThat(ex.getMessage()).isEqualTo(ErrorCode.WALLET_NOT_FOUND.getMessage());
                    });
        }

        @ParameterizedTest
        @ValueSource(longs = {0L, -1L, -10000L})
        void 충전_시_금액이_0이하이면_예외가_발생한다(Long invalidAmount) {
            // given
            Long memberId = 1L;
            WalletJpaEntity walletJpaEntity = WalletJpaEntity.builder()
                    .memberId(memberId)
                    .balance(0L)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            em.persist(walletJpaEntity);
            em.flush();
            em.clear();
            // when then
            assertThatThrownBy(() -> walletService.charge(memberId, invalidAmount))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_AMOUNT_INVALID);
                        assertThat(ex.getMessage()).isEqualTo(ErrorCode.WALLET_AMOUNT_INVALID.getMessage());
                    });
        }

        @ParameterizedTest
        @NullSource
        void 충전_시_금액이_null이면_예외가_발생한다(Long nullAmount) {
            //given
            Long memberId = 1L;
            WalletJpaEntity walletJpaEntity = WalletJpaEntity.builder()
                    .memberId(memberId)
                    .balance(0L)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            em.persist(walletJpaEntity);
            em.flush();
            em.clear();
            //when then
            assertThatThrownBy(() -> walletService.charge(memberId, nullAmount))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_AMOUNT_REQUIRED);
                assertThat(ex.getMessage()).isEqualTo(ErrorCode.WALLET_AMOUNT_REQUIRED.getMessage());
            });
        }
    }

    @Nested
    @DisplayName("지갑 출금 통합 예외 테스트")
    class DecreaseExceptionTest {

        @Test
        void 지갑이_없으면_출금_시_예외를_던지고_저장하지_않는다() {
            // given
            Long memberId = Long.MAX_VALUE;
            Long amount = 10000L;

            // when & then
            assertThatThrownBy(() -> walletService.decrease(memberId, amount))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_NOT_FOUND);
                        assertThat(ex.getMessage()).isEqualTo(ErrorCode.WALLET_NOT_FOUND.getMessage());
                    });
        }

        @ParameterizedTest
        @ValueSource(longs = {0L, -1L, -10000L})
        void 출금_시_금액이_0이하이면_예외가_발생한다(Long invalidAmount) {
            // given
            Long memberId = 1L;
            WalletJpaEntity walletJpaEntity = WalletJpaEntity.builder()
                    .memberId(memberId)
                    .balance(0L)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            em.persist(walletJpaEntity);
            em.flush();
            em.clear();
            // when then
            assertThatThrownBy(() -> walletService.decrease(memberId, invalidAmount))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_AMOUNT_INVALID);
                        assertThat(ex.getMessage()).isEqualTo(ErrorCode.WALLET_AMOUNT_INVALID.getMessage());
                    });
        }

        @ParameterizedTest
        @NullSource
        void 출금_시_금액이_null이면_예외가_발생한다(Long nullAmount) {
            //given
            Long memberId = 1L;
            WalletJpaEntity walletJpaEntity = WalletJpaEntity.builder()
                    .memberId(memberId)
                    .balance(0L)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            em.persist(walletJpaEntity);
            em.flush();
            em.clear();
            //when then
            assertThatThrownBy(() -> walletService.decrease(memberId, nullAmount))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_AMOUNT_REQUIRED);
                        assertThat(ex.getMessage()).isEqualTo(ErrorCode.WALLET_AMOUNT_REQUIRED.getMessage());
                    });
        }

        @Test
        void 잔액이_출금_금액보다_적으면_예외가_발생한다() {
            //given
            Long memberId = 1L;
            WalletJpaEntity walletJpaEntity = WalletJpaEntity.builder()
                    .memberId(memberId)
                    .balance(0L)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            em.persist(walletJpaEntity);
            em.flush();
            em.clear();
            //when then
            assertThatThrownBy(() -> walletService.decrease(memberId, 5000L))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_INSUFFICIENT_BALANCE);
                        assertThat(ex.getMessage()).isEqualTo(ErrorCode.WALLET_INSUFFICIENT_BALANCE.getMessage());
                    });
        }
    }
}
