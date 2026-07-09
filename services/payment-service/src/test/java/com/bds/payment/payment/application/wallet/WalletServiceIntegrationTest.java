package com.bds.payment.payment.application.wallet;

import com.bds.payment.payment.domain.wallet.Wallet;
import com.bds.payment.payment.domain.wallet.WalletRepository;
import com.bds.payment.payment.presentation.response.WalletResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class WalletServiceIntegrationTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Nested
    @DisplayName("지갑 조회 통합 테스트")
    class getWalletResponseDto{

        @Test
        public void 기존_지갑_조회가_정상_처리된다() {
            Long memberId = 1L;
            Wallet wallet = Wallet.builder()
                    .memberId(memberId)
                    .balance(5000L)
                    .build();

            walletRepository.save(wallet);

            WalletResponseDto result = walletService.getWalletResponseDto(memberId);

            assertThat(result).isNotNull();
            assertThat(result.memberId()).isEqualTo(memberId);
            assertThat(result.balance()).isEqualTo(5000L);
        }
    }

    @Nested
    @DisplayName("지갑 조회 통합 테스트")
    class createWallet{

        @Test
        public void 지갑_생성이_정상_처리된다() {
            Long memberId = 1L;

            WalletResponseDto result = walletService.createWallet(memberId);

            assertThat(result).isNotNull();
            assertThat(result.memberId()).isEqualTo(memberId);
            assertThat(result.balance()).isEqualTo(0L);
        }
    }

    @Test
    @DisplayName("지갑 충전을 정상 처리한다")
    void 지갑_충전을_정상_처리한다() {
        Long memberId = 1L;
        Long amount = 10000L;
        Wallet wallet = Wallet.builder()
                .memberId(memberId)
                .balance(0L)
                .build();
        walletRepository.save(wallet);

        Wallet result = walletService.charge(memberId, amount);

        assertThat(result).isNotNull();
        assertThat(result.getBalance()).isEqualTo(10000L);
    }

    @Test
    @DisplayName("지갑 출금이 정상 처리된다")
    void 지갑_출금을_정상_처리한다() {
        Long memberId = 1L;
        Long amount = 5000L;
        Wallet wallet = Wallet.builder()
                .memberId(memberId)
                .balance(20000L)
                .build();
        walletRepository.save(wallet);

        Wallet result = walletService.decrease(memberId, amount);

        assertThat(result).isNotNull();
        assertThat(result.getBalance()).isEqualTo(15000L);
    }
}
