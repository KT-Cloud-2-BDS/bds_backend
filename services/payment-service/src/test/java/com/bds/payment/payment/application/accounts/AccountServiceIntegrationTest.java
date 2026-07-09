package com.bds.payment.payment.application.accounts;

import com.bds.payment.payment.application.wallet.WalletService;
import com.bds.payment.payment.domain.account.Account;
import com.bds.payment.payment.domain.account.AccountRepository;
import com.bds.payment.payment.domain.wallet.Wallet;
import com.bds.payment.payment.domain.wallet.WalletRepository;
import com.bds.payment.payment.infrastructure.external.BankClient;
import com.bds.payment.payment.infrastructure.external.request.BankAccountRequestDto;
import com.bds.payment.payment.infrastructure.external.response.BankAccountResponseDto;
import com.bds.payment.payment.presentation.request.AccountRegisterRequestDto;
import com.bds.payment.payment.presentation.request.AccountVerifyRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AccountServiceIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @MockitoBean
    private BankClient client;

    @Test
    void registerAccount가_정상_동작한다() {
        Long memberId = 1L;
        walletRepository.save(Wallet.builder().memberId(memberId).balance(0L).build());
        AccountRegisterRequestDto dto = new AccountRegisterRequestDto("004", "1234567890", "홍길동");

        given(client.requestVerification(any(BankAccountRequestDto.class))).willReturn(new BankAccountResponseDto(dto.accountNumber(), dto.holderName()));

        String result = accountService.registerAccount(memberId, dto);

        assertThat(result).isEqualTo("정상 처리되었습니다.");
        assertThat(accountRepository.findById(walletService.getWalletId(memberId))).isPresent();
        verify(client).requestVerification(any(BankAccountRequestDto.class));
    }

    @Test
    void verifyAccount가_정상_동작한다() {
        Long memberId = 1L;
        walletRepository.save(Wallet.builder().memberId(memberId).balance(0L).build());

        AccountVerifyRequestDto dto = new AccountVerifyRequestDto("1234");

        Long walletId = walletService.getWalletId(memberId);

        Account account = Account.builder()
                .walletId(walletId)
                .bankCode("004")
                .accountNumber("1234567890")
                .holderName("홍길동")
                .build();
        accountRepository.save(account);

        given(client.confirmVerification(any())).willReturn(true);

        String result = accountService.verifyAccount(memberId, dto);

        assertThat(result).isEqualTo("정상 처리되었습니다.");
        verify(client).confirmVerification(any());
        assertThat(accountRepository.findById(walletId).orElseThrow().getIsVerified()).isTrue();
    }
}
