package com.bds.payment.payment.application.wallet;

import com.bds.payment.payment.domain.wallet.Wallet;
import com.bds.payment.payment.domain.wallet.WalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WalletServiceUnitExceptionTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletService walletService;

    @Nested
    @DisplayName("지갑 조회 예외 테스트")
    class GetWalletExceptionTest {

        @Test
        void 지갑이_없으면_getWalletResponseDto는_예외를_던진다() {
            // given
            Long memberId = 1L;
            given(walletRepository.findByMemberId(memberId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> walletService.getWalletResponseDto(memberId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("잘못된 접근입니다.");
        }

        @Test
        void 지갑이_없으면_getWallet은_예외를_던진다() {
            // given
            Long memberId = 1L;
            given(walletRepository.findByMemberId(memberId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> walletService.getWallet(memberId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("잘못된 접근입니다.");
        }

        @Test
        void 지갑이_없으면_getWalletId는_예외를_던진다() {
            // given
            Long memberId = 1L;
            given(walletRepository.findByMemberId(memberId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> walletService.getWalletId(memberId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("잘못된 접근입니다.");
        }
    }

    @Nested
    @DisplayName("지갑 생성 예외 테스트")
    class CreateWalletExceptionTest {

        @Test
        void 이미_지갑이_있으면_예외를_던지고_저장하지_않는다() {
            // given
            Long memberId = 1L;
            given(walletRepository.existsByMemberId(memberId)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> walletService.createWallet(memberId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("잘못된 접근입니다.");

            verify(walletRepository, never()).save(any(Wallet.class));
        }
    }

    @Nested
    @DisplayName("지갑 충전 예외 테스트")
    class ChargeExceptionTest {

        @Test
        void 지갑이_없으면_충전_시_예외를_던지고_저장하지_않는다() {
            // given
            Long memberId = Long.MAX_VALUE;
            Long amount = 10000L;
            given(walletRepository.findByMemberId(memberId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> walletService.charge(memberId, amount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("잘못된 접근입니다.");

            verify(walletRepository, never()).save(any(Wallet.class));
        }
    }

    @Nested
    @DisplayName("지갑 출금 예외 테스트")
    class DecreaseExceptionTest {

        @Test
        void 지갑이_없으면_출금_시_예외를_던지고_저장하지_않는다() {
            // given
            Long memberId = Long.MAX_VALUE;
            Long amount = 10000L;
            given(walletRepository.findByMemberId(memberId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> walletService.decrease(memberId, amount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("잘못된 접근입니다.");

            verify(walletRepository, never()).save(any(Wallet.class));
        }
    }
}
