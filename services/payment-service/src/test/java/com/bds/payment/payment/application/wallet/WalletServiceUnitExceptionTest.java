package com.bds.payment.payment.application.wallet;

import com.bds.payment.payment.domain.wallet.Wallet;
import com.bds.payment.payment.domain.wallet.WalletRepository;
import com.bds.payment.payment.global.exception.BusinessException;
import com.bds.payment.payment.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_NOT_FOUND);
                        assertThat(ex.getMessage()).isEqualTo(ErrorCode.WALLET_NOT_FOUND.getMessage());
                    });

        }

        @Test
        void 지갑이_없으면_getWallet은_예외를_던진다() {
            // given
            Long memberId = 1L;
            given(walletRepository.findByMemberId(memberId)).willReturn(Optional.empty());

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
            given(walletRepository.findByMemberId(memberId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> walletService.getWalletId(memberId))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_NOT_FOUND);
                        assertThat(ex.getMessage()).isEqualTo(ErrorCode.WALLET_NOT_FOUND.getMessage());
                    });
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
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_ALREADY_EXISTS);
                        assertThat(ex.getMessage()).isEqualTo(ErrorCode.WALLET_ALREADY_EXISTS.getMessage());
                    });

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
            given(walletRepository.findByMemberIdWithLock(memberId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> walletService.charge(memberId, amount))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_NOT_FOUND);
                        assertThat(ex.getMessage()).isEqualTo(ErrorCode.WALLET_NOT_FOUND.getMessage());
                    });

            verify(walletRepository, never()).save(any(Wallet.class));
        }

        @ParameterizedTest
        @ValueSource(longs = {0L, -1L, -10000L})
        void 금액이_0이하이면_예외가_발생한다(Long invalidAmount) {
            //given
            Long memberId = 1L;
            Wallet.create(memberId);
            //when then
            assertThatThrownBy(() -> walletService.charge(memberId, invalidAmount))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_AMOUNT_INVALID);
                        assertThat(ex.getMessage()).isEqualTo(ErrorCode.WALLET_AMOUNT_INVALID.getMessage());
                    });

            verify(walletRepository, never()).findByMemberIdWithLock(any());
            verify(walletRepository, never()).save(any());
        }

        @ParameterizedTest
        @NullSource
        void 금액이_null이면_예외가_발생한다(Long nullAmount) {
            //given
            Long memberId = 1L;
            Wallet.create(memberId);
            //when then
            assertThatThrownBy(() -> walletService.charge(memberId, nullAmount))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_AMOUNT_REQUIRED);
                        assertThat(ex.getMessage()).isEqualTo(ErrorCode.WALLET_AMOUNT_REQUIRED.getMessage());
                    });

            verify(walletRepository, never()).findByMemberIdWithLock(any());
            verify(walletRepository, never()).save(any());
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
            given(walletRepository.findByMemberIdWithLock(memberId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> walletService.decrease(memberId, amount))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_NOT_FOUND);
                        assertThat(ex.getMessage()).isEqualTo(ErrorCode.WALLET_NOT_FOUND.getMessage());
                    });

            verify(walletRepository, never()).save(any(Wallet.class));
        }

        @ParameterizedTest
        @ValueSource(longs = {0L, -1L, -10000L})
        void 금액이_0이하이면_예외가_발생한다(Long invalidAmount) {
            //given
            Long memberId = 1L;
            Wallet.create(memberId);
            //when then
            assertThatThrownBy(() -> walletService.decrease(memberId, invalidAmount))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_AMOUNT_INVALID);
                        assertThat(ex.getMessage()).isEqualTo(ErrorCode.WALLET_AMOUNT_INVALID.getMessage());
                    });

            verify(walletRepository, never()).findByMemberIdWithLock(any());
            verify(walletRepository, never()).save(any());
        }

        @ParameterizedTest
        @NullSource
        void 금액이_null이면_예외가_발생한다(Long nullAmount) {
            //given
            Long memberId = 1L;
            Wallet.create(memberId);
            //when then
            assertThatThrownBy(() -> walletService.decrease(memberId, nullAmount))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_AMOUNT_REQUIRED);
                        assertThat(ex.getMessage()).isEqualTo(ErrorCode.WALLET_AMOUNT_REQUIRED.getMessage());
                    });

            verify(walletRepository, never()).findByMemberIdWithLock(any());
            verify(walletRepository, never()).save(any());
        }

        @Test
        void 잔액이_출금_금액보다_적으면_예외가_발생한다() {
            //given
            Long memberId = 1L;
            Wallet wallet = Wallet.create(memberId);
            given(walletRepository.findByMemberIdWithLock(memberId)).willReturn(Optional.ofNullable(wallet));
            //when then
            assertThatThrownBy(() -> walletService.decrease(memberId, 5000L))
                    .isInstanceOfSatisfying(BusinessException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WALLET_INSUFFICIENT_BALANCE);
                        assertThat(ex.getMessage()).isEqualTo(ErrorCode.WALLET_INSUFFICIENT_BALANCE.getMessage());
                    });

            verify(walletRepository, never()).save(any());
        }
    }
}
