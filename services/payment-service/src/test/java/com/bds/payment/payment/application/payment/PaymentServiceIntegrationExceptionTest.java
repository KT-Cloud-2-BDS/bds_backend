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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Transactional
class PaymentServiceIntegrationExceptionTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private AccountRepository accountRepository;

    @MockitoBean
    private BankClient bankClient;

    @Test
    void 계좌를_찾을_수_없으면_charge_수행_중_예외가_발생한다() {
        Long memberId = 1L;
        AccountTransactionRequestDto dto = new AccountTransactionRequestDto(10000L);

        assertThatThrownBy(() -> paymentService.charge(memberId, dto))
                .isInstanceOf(IllegalArgumentException.class);

        verify(bankClient, never()).withdraw(any(BankTransactionRequestDto.class));
    }

    @Test
    void bankClient가_실패하면_charge_수행_중_예외가_발생한다() {
        //given
        Long memberId = 1L;
        Wallet wallet = Wallet.builder()
                .memberId(memberId)
                .balance(0L)
                .build();
        walletRepository.save(wallet);
        //when
        walletService.getWalletId(memberId);
        //then
        Account account = Account.builder()
                .walletId(walletService.getWalletId(memberId))
                .bankCode("004")
                .accountNumber("1234567890")
                .holderName("테스트")
                .build();
        accountRepository.save(account);

        AccountTransactionRequestDto dto = new AccountTransactionRequestDto(10000L);
        given(bankClient.withdraw(any(BankTransactionRequestDto.class))).willThrow(new IllegalStateException("bank unavailable"));

        assertThatThrownBy(() -> paymentService.charge(memberId, dto))
                .isInstanceOf(IllegalStateException.class);
    }
}
