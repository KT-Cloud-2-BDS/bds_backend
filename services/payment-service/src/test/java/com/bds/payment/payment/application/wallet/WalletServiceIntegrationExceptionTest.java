package com.bds.payment.payment.application.wallet;

import com.bds.payment.payment.infrastructure.persistence.wallet.WalletJpaEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

            assertThatThrownBy(() -> walletService.getWallet(memberId))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void 지갑이_없으면_getWallet은_예외를_던진다() {
            // given
            Long memberId = 1L;
            // when & then
            assertThatThrownBy(() -> walletService.getWallet(memberId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("잘못된 접근입니다.");
        }

        @Test
        void 지갑이_없으면_getWalletId는_예외를_던진다() {
            // given
            Long memberId = 1L;

            // when & then
            assertThatThrownBy(() -> walletService.getWalletId(memberId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("잘못된 접근입니다.");
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
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("잘못된 접근입니다.");

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
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("잘못된 접근입니다.");
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
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("잘못된 접근입니다.");
        }
    }
}
