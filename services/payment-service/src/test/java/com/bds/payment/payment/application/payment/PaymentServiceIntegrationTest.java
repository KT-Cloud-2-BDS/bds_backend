package com.bds.payment.payment.application.payment;

import com.bds.payment.payment.application.wallet.WalletService;
import com.bds.payment.payment.domain.account.Account;
import com.bds.payment.payment.domain.account.AccountRepository;
import com.bds.payment.payment.domain.wallet.Wallet;
import com.bds.payment.payment.domain.wallet.WalletRepository;
import com.bds.payment.payment.infrastructure.external.BankClient;
import com.bds.payment.payment.infrastructure.external.request.BankTransactionRequestDto;
import com.bds.payment.payment.presentation.request.AccountTransactionRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@Transactional
class PaymentServiceIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private WalletRepository walletRepository;

    @MockitoBean
    private BankClient bankClient;

    @Test
    void 페이_충전이_local_DB_통합_환경에서_정상_실행된다() {
        Long memberId = 1L;
        Wallet wallet = Wallet.builder()
                .memberId(memberId)
                .balance(0L)
                .build();
        wallet = walletRepository.save(wallet);

        Account account = Account.builder()
                .walletId(wallet.getId())
                .bankCode("004")
                .accountNumber("1234567890")
                .holderName("테스트")
                .build();
        accountRepository.save(account);

        AccountTransactionRequestDto dto = new AccountTransactionRequestDto(10000L);
        given(bankClient.withdraw(any(BankTransactionRequestDto.class))).willReturn(null);

        paymentService.charge(memberId, dto);

        Wallet updatedWallet = walletService.getWallet(memberId);
        assertThat(updatedWallet.getBalance()).isEqualTo(10000L);
    }

    @Test
    void 페이_출금이_local_DB_통합_환경에서_정상_실행된다() {
        Long memberId = 1L;
        Wallet wallet = Wallet.builder()
                .memberId(memberId)
                .balance(50000L)
                .build();
        wallet = walletRepository.save(wallet);

        Account account = Account.builder()
                .walletId(wallet.getId())
                .bankCode("004")
                .accountNumber("1234567890")
                .holderName("테스트")
                .build();
        accountRepository.save(account);

        AccountTransactionRequestDto dto = new AccountTransactionRequestDto(10000L);
        given(bankClient.deposit(any(BankTransactionRequestDto.class))).willReturn(null);

        paymentService.withdraw(memberId, dto);

        Wallet updatedWallet = walletService.getWallet(memberId);
        assertThat(updatedWallet.getBalance()).isEqualTo(40000L);
    }
}
