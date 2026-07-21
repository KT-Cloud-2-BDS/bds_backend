package com.bds.payment.payment.application.wallet;

import com.bds.payment.payment.domain.wallet.Wallet;
import com.bds.payment.payment.domain.wallet.WalletRepository;
import com.bds.payment.payment.presentation.response.WalletResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class WalletServiceUnitTest {

    @Mock private WalletRepository walletRepository;

    @InjectMocks private WalletService walletService;

    @Nested
    @DisplayName("지갑 조회 정상 테스트")
    class getWalletResponseDtoTest{

        @Test
        public void 저장된_지갑을_정상적으로_반한한다() {
            // given
            Long memberId = 1L;
            Wallet wallet = Wallet.builder()
                    .id(1L)
                    .memberId(memberId)
                    .balance(0L)
                    .build();
            given(walletRepository.findByMemberId(memberId)).willReturn(Optional.of(wallet));

            // when
            WalletResponseDto result = walletService.getWalletResponseDto(memberId);

            // then
            assertNotNull(result);
            assertEquals(memberId, result.memberId());
            assertEquals(0L, result.balance());
        }
    }

    @Nested
    @DisplayName("지갑 생성 정상 테스트")
    class CreateWalletTest{

        @Test
        public void 생성된_지갑을_정상적으로_반한한다() {
            // given
            Long memberId = 1L;
            Wallet wallet = Wallet.builder()
                    .id(1L)
                    .memberId(memberId)
                    .balance(0L)
                    .build();
            given(walletRepository.save(any(Wallet.class))).willReturn(wallet);
            // when
            WalletResponseDto result = walletService.createWallet(memberId);

            // then
            assertNotNull(result);
            assertEquals(memberId, result.memberId());
            assertEquals(0L, result.balance());
        }
    }

    @Nested
    @DisplayName("지갑 충전 정상 테스트")
    class chargeTest {

        @Test
        public void 지갑을_정상적으로_충전한다() {
            // given
            Long memberId = 1L;
            Long amount = 10000L;
            Wallet wallet = Wallet.builder()
                    .id(1L)
                    .memberId(memberId)
                    .balance(0L)
                    .build();
            given(walletRepository.findByMemberIdWithLock(memberId)).willReturn(Optional.of(wallet));
            given(walletRepository.save(wallet)).willReturn(wallet);

            // when
            Wallet result = walletService.charge(memberId, amount);

            // then
            assertNotNull(result);
            assertEquals(10000L, result.getBalance());
        }
    }

    @Nested
    @DisplayName("지갑 출금 정상 테스트")
    class decreaseTest {

        @Test
        public void 지갑을_정상적으로_출금한다() {
            // given
            Long memberId = 1L;
            Long amount = 10000L;
            Wallet wallet = Wallet.builder()
                    .id(1L)
                    .memberId(memberId)
                    .balance(50000L)
                    .build();
            given(walletRepository.findByMemberIdWithLock(memberId)).willReturn(Optional.of(wallet));
            given(walletRepository.save(wallet)).willReturn(wallet);

            // when
            Wallet result = walletService.decrease(memberId, amount);

            // then
            assertNotNull(result);
            assertEquals(40000L, result.getBalance());
        }
    }

    @Nested
    @DisplayName("지갑 ID 조회 정상 테스트")
    class getWalletIdTest {

        @Test
        public void 지갑_ID를_정상적으로_반환한다() {
            // given
            Long memberId = 1L;
            Wallet wallet = Wallet.builder()
                    .id(1L)
                    .memberId(memberId)
                    .balance(0L)
                    .build();
            given(walletRepository.findByMemberId(memberId)).willReturn(Optional.of(wallet));

            // when
            Long walletId = walletService.getWalletId(memberId);

            // then
            assertEquals(1L, walletId);
        }
    }
}